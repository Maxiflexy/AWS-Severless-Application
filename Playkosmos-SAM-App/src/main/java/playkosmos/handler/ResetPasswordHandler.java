package playkosmos.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.security.crypto.bcrypt.BCrypt;
import playkosmos.dao.UserDAO;
import playkosmos.dbutil.DatabaseConnectionManager;
import playkosmos.entity.User;
import playkosmos.utils.JwtUtils;
import playkosmos.utils.SecretsManagerHelper;
import software.amazon.awssdk.regions.Region;

import javax.crypto.SecretKey;
import java.sql.SQLException;
import java.util.Map;

public class ResetPasswordHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final SecretKey SECRET_KEY = JwtUtils.getSecretKey();
    private final SecretsManagerHelper secretsManagerHelper;
    private final Gson gson;

    public ResetPasswordHandler() {
        Region region = Region.of(System.getenv("REGION_NAME"));
        String secretName = System.getenv("DB_SECRET");

        this.secretsManagerHelper = SecretsManagerHelper.getInstance(String.valueOf(region), secretName);
        this.gson = new GsonBuilder().create();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {

        String username = (String) requestEvent.getRequestContext()
                .getAuthorizer()
                .get("principalId");

        if (username == null || username.isEmpty()) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody("Authorization failed: Missing username");
        }

        String requestBody = requestEvent.getBody();
        Map<String, String> requestMap = gson.fromJson(requestBody, Map.class);
        String newPassword = requestMap.get("newPassword");

        if (newPassword == null || newPassword.isEmpty()) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(gson.toJson(Map.of("status", "error", "message", "New password is required")));
        }

        try {

            String secret = secretsManagerHelper.getSecret();
            Map<String, Object> secretMap = gson.fromJson(secret, Map.class);

            DatabaseConnectionManager dcm = DatabaseConnectionManager.getInstance(secretMap);
            UserDAO userDAO = new UserDAO(dcm);

            User user = userDAO.findUserByUsername(username);
            if (user == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404)
                        .withBody(gson.toJson(Map.of("status", "error", "message", "User not found")));
            }

            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            userDAO.updateUserPassword(user.getUsername(), hashedPassword);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(Map.of("status", "success", "message", "Password updated successfully")));

        } catch (SQLException e) {
            LambdaLogger logger = context.getLogger();
            logger.log("Error in resetting password: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(gson.toJson(Map.of("status", "error", "message", e.getMessage())));
        }
    }
}

