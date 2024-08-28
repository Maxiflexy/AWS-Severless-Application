package playkosmos.handler;

import com.amazonaws.services.lambda.*;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import playkosmos.dao.UserDAO;
import playkosmos.dbutil.DatabaseConnectionManager;
import playkosmos.entity.User;
import playkosmos.utils.SecretsManagerHelper;
import software.amazon.awssdk.regions.Region;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.sql.SQLException;
import java.util.Map;

public class RequestOtpHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final SecretsManagerHelper secretsManagerHelper;
    private final Gson gson;
//    private final RedisClient redisClient;
//    private final StatefulRedisConnection<String, String> redisConnection;

    private final AWSLambda lambdaClient;

    public RequestOtpHandler() {
        Region region = Region.of(System.getenv("REGION_NAME"));
        String secretName = System.getenv("DB_SECRET");

        this.secretsManagerHelper = SecretsManagerHelper.getInstance(String.valueOf(region), secretName);
        this.gson = new GsonBuilder().create();

//        //String redisUrl = System.getenv("REDIS_URL");
//        String redisUrl = "redis://default:frUEafjmc0rM3HNQfDq9O6ec9B8UKbOX@redis-14917.c341.af-south-1-1.ec2.redns.redis-cloud.com:14917";
//        System.out.println(redisUrl);
//        this.redisClient = RedisClient.create(redisUrl);
//        this.redisConnection = redisClient.connect();

        this.lambdaClient = AWSLambdaClientBuilder.defaultClient();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        LambdaLogger logger = context.getLogger();
        String requestBody = requestEvent.getBody();
        Map<String, String> requestMap = gson.fromJson(requestBody, Map.class);

        String emailOrPhone = requestMap.get("emailOrPhone");
        logger.log(emailOrPhone);


        try {
            String secret = secretsManagerHelper.getSecret();
            Map<String, Object> secretMap = gson.fromJson(secret, Map.class);
            DatabaseConnectionManager dcm = DatabaseConnectionManager.getInstance(secretMap);
            UserDAO userDAO = new UserDAO(dcm);

            User user = isValidEmail(emailOrPhone) ? userDAO.findUserByEmail(emailOrPhone) : userDAO.findUserByPhoneNumber(emailOrPhone);
            if (user == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404)
                        .withBody(gson.toJson(Map.of("status", "error", "message", "User not found")));
            }

            String otp = generateOtp();

            logger.log(otp);
            logger.log("here now1");

            String userId = "user-id";  // Replace with actual user ID or another key

            String payload = gson.toJson(Map.of("key", "otp:" + userId, "value", otp));
            InvokeRequest invokeRequest = new InvokeRequest()
                    .withFunctionName("RedisConnectFunction")  // Name of the RedisConnect Lambda function
                    .withPayload(payload);

            InvokeResult invokeResult = lambdaClient.invoke(invokeRequest);

            String response = new String(invokeResult.getPayload().array(), StandardCharsets.UTF_8);

            //saveOtpToDatabase(userDAO, user, otp);
            //saveOtpToRedis(otp);
            logger.log("here now2");
            logger.log(response);
            //saveOtpToRedis(String.valueOf(user.getId()), otp);

            // Send OTP via email or SMS
            //sendOtpToUser(user, otp);

//            return new APIGatewayProxyResponseEvent()
//                    .withStatusCode(200)
//                    .withBody(gson.toJson(Map.of("status", "success", "message", "OTP sent")));

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(Map.of("status", "success", "otp", otp)));

        } catch (SQLException e) {
            logger = context.getLogger();
            logger.log("Error in requesting OTP: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(gson.toJson(Map.of("status", "error", "message", e.getMessage())));
        }
    }

    private String generateOtp() {
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }

    private void saveOtpToDatabase(UserDAO userDAO, User user, String otp) throws SQLException {
        userDAO.saveOtpToDatabase(user, otp);
    }

//    private void saveOtpToRedis(String otp) {
//        RedisCommands<String, String> commands = redisConnection.sync();
//        String key = "otp:" + "user-id";
//        commands.set(key, otp);
//        commands.expire(key, 300);  // Set OTP to expire in 5 minutes (300 seconds)
//    }

//    @Override
//    public void close() {
//        if (redisConnection != null) {
//            redisConnection.close();
//        }
//        if (redisClient != null) {
//            redisClient.shutdown();
//        }
//    }

    private void sendOtpToUser(User user, String otp) {
        if (isValidEmail(user.getEmail())) {
            sendOtpViaEmail(user.getEmail(), otp);
        } else if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
            sendOtpViaSms(user.getPhoneNumber(), otp);
        }
    }

    private boolean isValidEmail(String input) {
        return input.contains("@");
    }

    private void sendOtpViaEmail(String email, String otp) {

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.smtp.ssl.enable", "true");


        final String username = "onyemax247@gmail.com";
        final String password = "wspfjavzhxawgyzw";

        // Create a session with the mail server
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("Your OTP Code");
            message.setText("Dear User,\n\nYour OTP code is: " + otp + "\n\nThank you.");

            // Send the email
            Transport.send(message);
            System.out.println("OTP sent via email successfully");

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send OTP via email", e);
        }
    }

    private void sendOtpViaSms(String phoneNumber, String otp) {

        try (SnsClient snsClient = SnsClient.builder()
                .region(Region.of(System.getenv("AWS_REGION")))
                .build()) {

            String message = "Your OTP code is: " + otp;

            PublishRequest request = PublishRequest.builder()
                    .message(message)
                    .phoneNumber(phoneNumber)
                    .build();

            PublishResponse result = snsClient.publish(request);
            System.out.println("OTP sent via SMS. Message ID: " + result.messageId());

        } catch (Exception e) {
            throw new RuntimeException("Failed to send OTP via SMS", e);
        }
    }
}

/*
Based on the above parallel pulling of information from the database, Create a  table and a lamnda function that when called can
pull information froom a post table in paraller.

1. The User details (profile picture, full name, location, icon/ rank of the user)

2. The description of the post and the users tagged to the post

3. The list of pictures that is associated with the post

4. The number of likes, number of shares, the number of comments, Q & A, Participant, Reviews

5. The number of Users that are attending or associated with a particular post

Given below is my UserDAO, update it and create a PostDAO that handles the creation and of all the various tables
associated with the post
*/