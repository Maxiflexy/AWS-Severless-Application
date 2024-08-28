package playkosmos.handler.testing;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class TestInternetAccessHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String result;
        try {
            URL url = new URL("https://www.google.com");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            int responseCode = urlConnection.getResponseCode();
            result = responseCode == 200 ? "Internet Access: Yes" : "Internet Access: No";
        } catch (IOException e) {
            result = "Internet Access: No - " + e.getMessage();
        }

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(result);
    }
}

