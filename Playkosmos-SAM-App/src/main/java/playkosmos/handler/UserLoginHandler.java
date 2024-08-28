package playkosmos.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import playkosmos.dao.UserDAO;
import playkosmos.dbutil.DatabaseConnectionManager;
import playkosmos.entity.User;
import playkosmos.utils.JwtUtils;
import playkosmos.utils.SecretsManagerHelper;
import software.amazon.awssdk.regions.Region;

import java.sql.SQLException;
import java.util.Map;

public class UserLoginHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final SecretsManagerHelper secretsManagerHelper;
    private final Gson gson;
    private final PasswordEncoder passwordEncoder;

    public UserLoginHandler() {
        Region region = Region.of(System.getenv("REGION_NAME"));
        String secretName = System.getenv("DB_SECRET");

        this.secretsManagerHelper = SecretsManagerHelper.getInstance(String.valueOf(region), secretName);
        this.gson = new GsonBuilder().create();
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {

        Map<String, String> requestBody = gson.fromJson(requestEvent.getBody(), Map.class);
        String emailOrPhone = requestBody.get("email") != null ? requestBody.get("email") : requestBody.get("phoneNumber");
        String password = requestBody.get("password");

        if (emailOrPhone == null || password == null) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(gson.toJson(Map.of("status", "error", "message", "Email or Phone number and password are required")));
        }


        try {
            String secret = secretsManagerHelper.getSecret();
            Map<String, Object> secretMap = gson.fromJson(secret, Map.class);

            DatabaseConnectionManager dcm = DatabaseConnectionManager.getInstance(secretMap);
            UserDAO userDAO = new UserDAO(dcm);

            User user;
            if (isValidEmail(emailOrPhone)) {
                user = userDAO.findUserByEmail(emailOrPhone);
            } else {
                user = userDAO.findUserByPhoneNumber(emailOrPhone);
            }


            if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(401)
                        .withBody(gson.toJson(Map.of("status", "error", "message", "Invalid credentials")));
            }

            String username = user.getUsername();
            String token = JwtUtils.generateToken(username);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(Map.of("status", "success", "token", token)));

        } catch (SQLException e) {
            LambdaLogger logger = context.getLogger();
            logger.log("Error during login: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(gson.toJson(Map.of("status", "error", "message", e.getMessage())));
        }
    }

    private boolean isValidEmail(String input) {
        return input.contains("@");
    }
}
