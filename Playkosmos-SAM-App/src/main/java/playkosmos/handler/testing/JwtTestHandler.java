package playkosmos.handler.testing;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.SignatureException;
import playkosmos.utils.JwtUtils;

import javax.crypto.SecretKey;


public class JwtTestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final SecretKey SECRET_KEY = JwtUtils.getSecretKey();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {

        String token = requestEvent.getHeaders().get("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody("Missing or invalid Authorization header");
        }

        token = token.substring(7);

        try {

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();

            if (username != null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody("Token is valid for user with given details: " + username);
            } else {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(401)
                        .withBody("Invalid token: Unrecognized identifier");
            }

        } catch (SignatureException e) {
            context.getLogger().log("SignatureException: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody("Invalid token");
        } catch (Exception e) {
            context.getLogger().log("General Exception: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Error: " + e.getMessage());
        }
    }

}
