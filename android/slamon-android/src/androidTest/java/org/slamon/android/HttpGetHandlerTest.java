package org.slamon.android;

import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.slamon.android.handlers.HttpGetHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;

/**
 * Tests for HTTP GET Handler
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class HttpGetHandlerTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    HttpGetHandler handler;
    HttpResponse response;

    @Before
    public void setup() {
        handler = spy(new HttpGetHandler());
    }

    @Test
    public void testNoInput() throws Exception {
        Map<String, Object> map = new HashMap<>();
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("No url");
        handler.execute(map);
    }

    @Test
    public void testWrongSyntaxUrl() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("url", "not valid url !#¤%&/()");
        expectedException.expect(IllegalArgumentException.class);
        Map<String, Object> result = handler.execute(map);
    }

    @Test
    public void testHttpError() throws Exception {
        handler.requestFactory = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        return new MockLowLevelHttpResponse().setStatusCode(500);
                    }
                };
            }
        }.createRequestFactory();
        Map<String, Object> map = new HashMap<>();
        map.put("url", "http://url.to.be.tested");
        expectedException.expect(HttpResponseException.class);
        Map<String, Object> result = handler.execute(map);
    }

    @Test
    public void testHttpOk() throws Exception {
        handler.requestFactory = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        return new MockLowLevelHttpResponse().setStatusCode(200);
                    }
                };
            }
        }.createRequestFactory();
        Map<String, Object> map = new HashMap<>();
        map.put("url", "http://url.to.be.tested");
        Map<String, Object> result = handler.execute(map);
        assertEquals(200, result.get("status"));
    }

}
