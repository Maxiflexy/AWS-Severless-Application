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
import playkosmos.utils.LocalDateTypeAdapter;
import playkosmos.utils.SecretsManagerHelper;
import playkosmos.utils.ValidationResult;
import playkosmos.utils.ValidationUtils;
import software.amazon.awssdk.regions.Region;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class UserRegistrationHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final SecretsManagerHelper secretsManagerHelper;
    private final Gson gson;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationHandler() {

        Region region = Region.of(System.getenv("REGION_NAME"));
        String secretName = System.getenv("DB_SECRET");

        this.secretsManagerHelper = SecretsManagerHelper.getInstance(String.valueOf(region), secretName);
        this.gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter()).create();
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {

        User user = gson.fromJson(requestEvent.getBody(), User.class);

        CompletableFuture<ValidationResult> usernameValid = ValidationUtils.validateUsername(user.getUsername());
        CompletableFuture<ValidationResult> contactValid = ValidationUtils.validateContact(user.getEmail(), user.getPhoneNumber(), user.getCountryCode());
        CompletableFuture<ValidationResult> dobValid = ValidationUtils.validateDateOfBirth(user.getDateOfBirth());
        CompletableFuture<ValidationResult> passwordValid = ValidationUtils.validatePassword(user.getPassword());

        try {

            CompletableFuture<Void> allOf = CompletableFuture.allOf(usernameValid, contactValid, dobValid, passwordValid);
            allOf.get();

            StringBuilder validationErrors = new StringBuilder();

            if (!usernameValid.get().isValid()) {
                validationErrors.append(usernameValid.get().getMessage());
            }
            if (!contactValid.get().isValid()) {
                validationErrors.append(contactValid.get().getMessage());
            }
            if (!dobValid.get().isValid()) {
                validationErrors.append(dobValid.get().getMessage());
            }
            if (!passwordValid.get().isValid()) {
                validationErrors.append(passwordValid.get().getMessage());
            }

            if (!validationErrors.isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody(gson.toJson(Map.of("status", "error", "message", validationErrors.toString().trim())));
            }

            user.setPassword(passwordEncoder.encode(user.getPassword()));

            String secret = secretsManagerHelper.getSecret();
            Map<String, Object> secretMap = gson.fromJson(secret, Map.class);

            DatabaseConnectionManager dcm = DatabaseConnectionManager.getInstance(secretMap);
            UserDAO userDAO = new UserDAO(dcm);
            userDAO.saveUserToDatabase(user);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(Map.of("status", "successfully signup")));

        } catch (InterruptedException | ExecutionException | SQLException e) {
            LambdaLogger logger = context.getLogger();
            logger.log("Error saving user: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(gson.toJson(Map.of("status", "error", "message", e.getMessage())));
        }
    }
}