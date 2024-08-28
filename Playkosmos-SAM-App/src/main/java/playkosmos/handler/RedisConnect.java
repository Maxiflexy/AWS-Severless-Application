package playkosmos.handler;

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

public class RedisConnect implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>, AutoCloseable {

    private final Gson gson;
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> redisConnection;

    public RedisConnect() {
        this.gson = new GsonBuilder().create();
        //String redisUrl = System.getenv("REDIS_URL");
        String redisUrl = "redis://default:frUEafjmc0rM3HNQfDq9O6ec9B8UKbOX@redis-14917.c341.af-south-1-1.ec2.redns.redis-cloud.com:14917";
        this.redisClient = RedisClient.create(redisUrl);
        this.redisConnection = redisClient.connect();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        String requestBody = requestEvent.getBody();
        Map<String, String> requestMap = gson.fromJson(requestBody, Map.class);

        String key = requestMap.get("key");
        String value = requestMap.get("value");

        try {
            RedisCommands<String, String> commands = redisConnection.sync();
            commands.set(key, value);
            commands.expire(key, 300);  // Set OTP to expire in 5 minutes (300 seconds)

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(Map.of("status", "success", "message", "Data stored in Redis")));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(gson.toJson(Map.of("status", "error", "message", e.getMessage())));
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