package playkosmos.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import playkosmos.utils.LocalDateTimeTypeAdapter;
import playkosmos.utils.LocalDateTypeAdapter;
import playkosmos.utils.SecretsManagerHelper;
import software.amazon.awssdk.regions.Region;

import java.time.LocalDate;
import java.time.LocalDateTime;

public abstract class BaseHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    protected final SecretsManagerHelper secretsManagerHelper;
    protected final Gson gson;
    protected final PasswordEncoder passwordEncoder;

    public BaseHandler() {
        Region region = Region.of(System.getenv("REGION_NAME"));
        String secretName = System.getenv("DB_SECRET");

        this.secretsManagerHelper = SecretsManagerHelper.getInstance(String.valueOf(region), secretName);
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
                .create();
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public abstract APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context);
}
