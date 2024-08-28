package playkosmos.handler;

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
import playkosmos.utils.JwtUtils;
import playkosmos.utils.SecretsManagerHelper;
import software.amazon.awssdk.regions.Region;

import java.sql.SQLException;
import java.util.Map;

public class VerifyOtpHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final SecretsManagerHelper secretsManagerHelper;
    private final Gson gson;

    public VerifyOtpHandler() {
        Region region = Region.of(System.getenv("REGION_NAME"));
        String secretName = System.getenv("DB_SECRET");

        this.secretsManagerHelper = SecretsManagerHelper.getInstance(String.valueOf(region), secretName);
        this.gson = new GsonBuilder().create();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {

        Map<String, String> requestBody = gson.fromJson(requestEvent.getBody(), Map.class);
        String emailOrPhone = requestBody.get("email") != null ? requestBody.get("email") : requestBody.get("phoneNumber");
        String otp = requestBody.get("otp");

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

            if (!userDAO.isOtpValid(user, otp)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(401)
                        .withBody(gson.toJson(Map.of("status", "error", "message", "Invalid OTP")));
            }

            String token = JwtUtils.generateToken(user.getUsername());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(Map.of("status", "success", "token", token)));

        } catch (SQLException e) {
            LambdaLogger logger = context.getLogger();
            logger.log("Error in verifying OTP: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(gson.toJson(Map.of("status", "error", "message", e.getMessage())));
        }
    }

    private boolean isValidEmail(String input) {
        return input.contains("@");
    }
}
