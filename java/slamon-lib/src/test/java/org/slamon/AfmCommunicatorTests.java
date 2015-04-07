package org.slamon;

import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.Json;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * Unit tests for Agent Fleet Manager communication
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpResponse.class, AfmCommunicator.class})
public class AfmCommunicatorTests {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    String valid_response_json = "{" +
            " \"return_time\": \"2015-02-17T22:19:41.620000+0200\"," +
            " \"tasks\": [" +
            "       {" +
            "            \"task_id\": \"TASK_UUID\"," +
            "            \"task_type\": \"android-wait\"," +
            "            \"task_version\": 1," +
            "            \"task_data\": {" +
            "               \"time\": 2" +
            "            }" +
            "       }" +
            "] " +
            "}";
    String invalid_response_json = "{" +
            " \"return_time\": \"Not proper date time\"," +
            " \"tasks\": [" +
            "       {" +
            "            \"task_id\": \"TASK_UUID\"," +
            "            \"task_type\": \"android-wait\"," +
            "            \"task_version\": 1," +
            "            \"task_data\": {" +
            "               \"time\": 2" +
            "            }" +
            "           }" +
            "] " +
            "}";
    AfmCommunicator mockCommunicator;
    HttpResponse mockResponse;

    @Before
    public void setup() throws Exception {
        mockCommunicator = spy(new AfmCommunicator("http://slamon.address.to.be"));
        mockResponse = mock(HttpResponse.class);
    }

    @Test
    public void testIOError() throws Exception {
        mockCommunicator.httpRequests = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    public LowLevelHttpResponse execute() throws IOException {
                        throw new IOException("test");
                    }
                };
            }
        }.createRequestFactory();
        expectedException.expect(AfmCommunicator.TemporaryException.class);
        mockCommunicator.requestTasks("", "", new HashMap<String, Integer>(), 1, new ArrayList<Task>());
    }

    @Test
    public void testServerError() throws Exception {
        mockCommunicator.httpRequests = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse result = new MockLowLevelHttpResponse();
                        result.setStatusCode(500);
                        return result;
                    }
                };
            }
        }.createRequestFactory();
        expectedException.expect(AfmCommunicator.TemporaryException.class);
        mockCommunicator.requestTasks("", "", new HashMap<String, Integer>(), 1, new ArrayList<Task>());
    }

    @Test
    public void testClientError() throws Exception {
        mockCommunicator.httpRequests = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse result = new MockLowLevelHttpResponse();
                        result.setStatusCode(400);
                        return result;
                    }
                };
            }

        }.createRequestFactory();
        expectedException.expect(AfmCommunicator.FatalException.class);
        mockCommunicator.requestTasks("", "", new HashMap<String, Integer>(), 1, new ArrayList<Task>());
    }

    @Test
    public void testJsonError() throws Exception {
        mockCommunicator.httpRequests = new JsonMockHttpTransport(invalid_response_json).createRequestFactory();
        List<Task> list = new ArrayList<Task>();
        expectedException.expect(AfmCommunicator.FatalException.class);
        expectedException.expectMessage("JSON");
        mockCommunicator.requestTasks("", "", new HashMap<String, Integer>(), 1, list);
    }

    @Test
    public void testValidJSON() throws Exception {
        mockCommunicator.httpRequests = new JsonMockHttpTransport(valid_response_json).createRequestFactory();
        List<Task> list = new ArrayList<Task>();
        DateTime testResult = mockCommunicator.requestTasks("", "", new HashMap<String, Integer>(), 1, list);
        Map<String, Object> taskData = new HashMap<String, Object>();
        taskData.put("time", 2.0);

        assertEquals(testResult.toString(), "2015-02-17T22:19:41.620+02:00");
        assertTrue(list.size() == 1);
        assertEquals(list.get(0).task_id, "TASK_UUID");
        assertEquals(list.get(0).task_type, "android-wait");
        assertEquals(list.get(0).task_version, new Double(1.0));
        assertEquals(list.get(0).task_data, taskData);
    }

    class JsonMockHttpTransport extends MockHttpTransport {
        String content;

        JsonMockHttpTransport(String content) {
            this.content = content;
        }

        @Override
        public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
            return new MockLowLevelHttpRequest() {
                @Override
                public LowLevelHttpResponse execute() throws IOException {
                    MockLowLevelHttpResponse result = new MockLowLevelHttpResponse();
                    result.setContent(Json.MEDIA_TYPE);
                    result.setContent(content);
                    return result;
                }
            };
        }
    }

}
