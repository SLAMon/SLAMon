package org.slamon;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * JBoss Unified Push Server client class for sending push notification.
 */
public class Jups {

    static final Logger log = Logger.getLogger(Jups.class.getCanonicalName());

    static final JsonFactory sJsonFactory = new GsonFactory();

    final String mUrl;
    final String mAppId;
    final String mSecret;
    HttpTransport mHttpTransport = new NetHttpTransport();

    public Jups(String url, String appId, String secret) {
        mUrl = url;
        mAppId = appId;
        mSecret = secret;
    }

    /**
     * Send push notification via JBoss Unified Push Server RESTful API
     */
    public String send(PushNotification notification, String testId) {

        try {
            StringBuilder url = new StringBuilder(mUrl);
            if (mUrl.charAt(mUrl.length() - 1) != '/') {
                url.append('/');
            }
            url.append("rest/sender");
            GenericUrl postUrl = new GenericUrl(url.toString());

            log.log(Level.INFO, "Seding post notification for test id {0} via {1}...", new Object[]{testId, mUrl});

            HttpRequestFactory factory = mHttpTransport.createRequestFactory();
            HttpRequest request = factory.buildPostRequest(postUrl, new JsonHttpContent(sJsonFactory, notification));
            request.getHeaders().setBasicAuthentication(mAppId, mSecret);
            request.getHeaders().setUserAgent("SLAMon JBoss Unified Push Server Handler");
            request.execute();
            // according to documentation, .execute() will throw HttpResponseException if
            // http status code >= 300 is received.

            return "successful";
        } catch (HttpResponseException e) {
            log.log(Level.SEVERE, "Failed to send push notification for test id {0}: {1}.", new Object[]{testId, e});
            return "failed";
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to send push notification for test id {0}: {1}.", new Object[]{testId, e});
            return "failed";
        }
    }
}
