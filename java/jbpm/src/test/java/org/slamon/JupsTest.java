package org.slamon;

import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.testing.http.HttpTesting;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class JupsTest {

   @Test
    public void testSuccessResult() throws InterruptedException, ExecutionException, TimeoutException {

        Jups jups = new Jups(HttpTesting.SIMPLE_URL, "APP_ID", "SECRET");
        jups.mHttpTransport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse result = new MockLowLevelHttpResponse();
                        result.setStatusCode(200);
                        return result;
                    }
                };
            }
        };

        String result = jups.send(new PushNotification("MASTER_VARIANT_ID", "VARIANT_ID", "ALERT_MESSAGE", "SOUND"), "TEST_UUID");
        assertEquals("Push notification should have succeeded", result, "successful");
    }

    @Test
    public void testPostIOError() throws InterruptedException, ExecutionException, TimeoutException {

    	Jups jups = new Jups(HttpTesting.SIMPLE_URL, "APP_ID", "SECRET");
        jups.mHttpTransport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        throw new IOException("test");
                    }
                };
            }
        };

        String result = jups.send(new PushNotification("MASTER_VARIANT_ID", "VARIANT_ID", "ALERT_MESSAGE", "SOUND"), "TEST_UUID");
        assertEquals("Push notification should have failed", result, "failed");
    }

    @Test
    public void testBadRequest() throws InterruptedException, ExecutionException, TimeoutException {

        Jups jups = new Jups(HttpTesting.SIMPLE_URL, "APP_ID", "SECRET");
        jups.mHttpTransport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse result = new MockLowLevelHttpResponse();
                        result.setStatusCode(400);
                        return result;
                    }
                };
            }
        };

        String result = jups.send(new PushNotification("MASTER_VARIANT_ID", "VARIANT_ID", "ALERT_MESSAGE", "SOUND"), "TEST_UUID");
        assertEquals("Push notification should have failed", result, "failed");
    }
}

