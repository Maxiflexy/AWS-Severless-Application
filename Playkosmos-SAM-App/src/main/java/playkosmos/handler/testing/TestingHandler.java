package playkosmos.handler.testing;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.util.Map;

public class TestingHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>, AutoCloseable {

    private final Gson gson;
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> redisConnection;

    public TestingHandler() {
        this.gson = new GsonBuilder().create();

        // Provide the correct Redis URL here
        String redisUrl = "redis://default:frUEafjmc0rM3HNQfDq9O6ec9B8UKbOX@redis-14917.c341.af-south-1-1.ec2.redns.redis-cloud.com:14917";
        this.redisClient = RedisClient.create(redisUrl);
        this.redisConnection = redisClient.connect();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        RedisCommands<String, String> commands = redisConnection.sync();

        // Generating a test key-value pair
        String testKey = "testKey";
        String testValue = "testValue" + Math.random();  // Random value for uniqueness

        // Storing the key-value pair in Redis
        commands.set(testKey, testValue);

        // Retrieving the value from Redis to confirm it was stored correctly
        String retrievedValue = commands.get(testKey);

        // Checking if the value was stored and retrieved correctly
        if (testValue.equals(retrievedValue)) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(Map.of("status", "success", "storedValue", retrievedValue)));
        } else {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(gson.toJson(Map.of("status", "error", "message", "Failed to store or retrieve data from Redis")));
        }
    }

    @Override
    public void close() {
        if (redisConnection != null) {
            redisConnection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }
}
