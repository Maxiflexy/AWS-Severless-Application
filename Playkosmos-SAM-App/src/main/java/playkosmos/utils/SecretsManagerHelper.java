package playkosmos.utils;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

@RequiredArgsConstructor
public class SecretsManagerHelper {

    private static SecretsManagerHelper instance;
    private final SecretsManagerClient secretsClient;
    private final String secretName;

    public SecretsManagerHelper(String region, String secretName) {
        this.secretsClient = SecretsManagerClient.builder()
                .region(Region.of(region))
                .build();
        this.secretName = secretName;
    }

    public String getSecret() {
        GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();
        GetSecretValueResponse getSecretValueResponse;

        try {
            getSecretValueResponse = secretsClient.getSecretValue(getSecretValueRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve secret: " + secretName, e);
        }

        return getSecretValueResponse.secretString();
    }

    public static synchronized SecretsManagerHelper getInstance(String region, String secretName) {
        if (instance == null) {
            instance = new SecretsManagerHelper(region, secretName);
        }
        return instance;
    }

}
