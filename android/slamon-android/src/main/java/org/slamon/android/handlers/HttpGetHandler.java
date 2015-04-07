package org.slamon.android.handlers;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.slamon.TaskHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * An example task handler that uses HTTP GET to provided url and reports the status code
 */
public class HttpGetHandler extends TaskHandler {
    public HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();

    @Override
    public Map<String, Object> execute(Map<String, Object> inputParams) throws Exception {
        String url = (String) inputParams.get("url");
        if (url == null) {
            throw new IllegalArgumentException("No url parameter found");
        }

        HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(url));

        Map<String, Object> result = new HashMap<>();
        try {
            HttpResponse response = request.execute();
            result.put("status", response.getStatusCode());
        } catch (HttpResponseException e) {
            result.put("status", e.getStatusCode());
        }

        return result;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public String getName() {
        return "url_http_status";
    }
}
