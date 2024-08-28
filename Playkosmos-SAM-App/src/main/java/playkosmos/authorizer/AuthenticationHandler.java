package playkosmos.authorizer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayCustomAuthorizerEvent;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import playkosmos.utils.JwtUtils;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AuthenticationHandler implements RequestHandler<APIGatewayCustomAuthorizerEvent, Map<String, Object>> {

    private static final SecretKey SECRET_KEY = JwtUtils.getSecretKey();

    @Override
    public Map<String, Object> handleRequest(APIGatewayCustomAuthorizerEvent request, Context context) {

        LambdaLogger logger = context.getLogger();
        String authToken = request.getAuthorizationToken();

        logger.log("authToken is: "  + authToken);

        if(authToken == null || !authToken.startsWith("Bearer ")){
            logger.log("missing authToken!!");
            return generatePolicy("user", "Deny", request.getMethodArn(), "Authorization incorrect");
        }


        String token = authToken.substring(7);

        try{
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            logger.log("Extracted username: " + username);

            if(username == null){
                logger.log("Username is null, invalid token.");
                throw new RuntimeException("Invalid token: Missing subject");
            }
            return generatePolicy(username, "Allow", request.getMethodArn());
        }catch (Exception e){
            logger.log("Token validation error: " + e.getMessage());
            return generatePolicy("user", "Deny", request.getMethodArn(), e.getMessage());
        }
    }

    private Map<String, Object> generatePolicy(String user, String effect, String resource) {
        return generatePolicy(user, effect, resource, null);
    }

    private Map<String, Object> generatePolicy(String principalId, String effect, String resource, String message) {

        Map<String, Object> policyDocument = new HashMap<>();
        policyDocument.put("Version", "2012-10-17");

        Map<String, Object> statement = new HashMap<>();
        statement.put("Effect", effect);
        statement.put("Action", "execute-api:Invoke");
        statement.put("Resource", resource);

        policyDocument.put("Statement", Collections.singletonList(statement));

        Map<String, Object> response = new HashMap<>();
        response.put("principalId", principalId);
        response.put("policyDocument", policyDocument);

        if(message != null){
            Map<String, String> context = new HashMap<>();
            context.put("message", message);
            response.put("context", context);
        }
        return response;
    }
}
