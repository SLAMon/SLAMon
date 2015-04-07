package org.slamon;

import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.Json;
import com.google.api.client.testing.http.HttpTesting;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class AfmTest {

    private static String waiting_task_json =
            "{" +
                    "   \"task_id\": \"TASK_UUID\"," +
                    "   \"test_id\": \"TEST_UUID\"," +
                    "   \"task_type\": \"wait\"," +
                    "   \"task_version\": 1," +
                    "   \"task_data\": {" +
                    "       \"time\": 2" +
                    "   }" +
                    "}";

    private static String failed_task_json =
            "{" +
                    "   \"task_id\": \"TASK_UUID\"," +
                    "   \"test_id\": \"TEST_UUID\"," +
                    "   \"task_type\": \"wait\"," +
                    "   \"task_version\": 1," +
                    "   \"task_data\": {" +
                    "       \"time\": 2" +
                    "   }," +
                    "   \"task_failed\": \"2015-01-17 18:14:04.345757\"," +
                    "   \"task_error\": \"error description\"" +
                    "}";

    private static String succeeded_task_json =
            "{" +
                    "   \"task_id\": \"TASK_UUID\"," +
                    "   \"test_id\": \"TEST_UUID\"," +
                    "   \"task_type\": \"wait\"," +
                    "   \"task_version\": 1," +
                    "   \"task_data\": {" +
                    "       \"time\": 2" +
                    "   }," +
                    "   \"task_completed\": \"2015-01-17 18:14:04.345757\"," +
                    "   \"task_result\": {" +
                    "       \"waited\": 2," +
                    "       \"waited-double\": 2.5," +
                    "       \"string-value\": \"string-value\"" +
                    "   }" +
                    "}";

    private static void checkTaskBasics(Task task) {
        assertEquals("TASK_UUID", task.task_id);
        assertEquals("TEST_UUID", task.test_id);
        assertEquals("wait", task.task_type);
        assertEquals(1, task.task_version);
        assertNotNull(task.task_data);
        assertEquals(new java.math.BigDecimal(2), task.task_data.get("time"));
    }

    class JsonMockHttpTransport extends MockHttpTransport {

        String mContent;
        int mRequestsCount;

        JsonMockHttpTransport(String content) {
            mContent = content;
            mRequestsCount = 0;
        }

        public int getRequestsCount() {
            return mRequestsCount;
        }

        @Override
        public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
            return new MockLowLevelHttpRequest() {
                @Override
                public LowLevelHttpResponse execute() throws IOException {
                    mRequestsCount++;
                    MockLowLevelHttpResponse result = new MockLowLevelHttpResponse();
                    result.setContentType(Json.MEDIA_TYPE);
                    result.setContent(mContent);
                    return result;
                }
            };
        }
    }

    class FutureCallback implements Afm.ResultCallback {
        boolean succeeded = false;
        final CompletableFuture<Task> task = new CompletableFuture<Task>();

        @Override
        public void succeeded(Task task) {
            succeeded = true;
            this.task.complete(task);
        }

        @Override
        public void failed(Task task) {
            this.task.complete(task);
        }
    }

    @Before
    public void ressetAfm() {
        Afm.resetAll();
    }

    @Test
    public void testSuccessResult() throws InterruptedException, ExecutionException, TimeoutException {

        Afm afm = Afm.get(HttpTesting.SIMPLE_URL);
        JsonMockHttpTransport mockTransport = new JsonMockHttpTransport(succeeded_task_json);
        afm.mHttpTransport = mockTransport;

        FutureCallback result = new FutureCallback();
        afm.postTask(new Task("TASK_UUID", "TEST_UUID", "wait", 1), result);

        Task resultTask = result.task.get(5000, TimeUnit.MILLISECONDS);
        assertNotNull("Task should have been returned", resultTask);
        assertTrue("Task should have succeeded", result.succeeded);
        assertNull("Result should not contain error.", resultTask.task_error);
        assertEquals("Afm should have only made 2 requests.", 2, mockTransport.getRequestsCount());
        checkTaskBasics(resultTask);
        assertNotNull("Task should have result data", resultTask.task_result);
        assertEquals("result value should match", new BigDecimal(2), resultTask.task_result.get("waited"));
        assertEquals("int result value should have zero", 0, ((BigDecimal) resultTask.task_result.get("waited")).scale());
        assertEquals("result value should match", new BigDecimal(2.5), resultTask.task_result.get("waited-double"));
        assertNotEquals("double return value should not have zero scale", 0, ((BigDecimal) resultTask.task_result.get("waited-double")).scale());
        assertEquals("result value should match", "string-value", resultTask.task_result.get("string-value"));
    }

    @Test
    public void testFailedResult() throws InterruptedException, ExecutionException, TimeoutException {

        Afm afm = Afm.get(HttpTesting.SIMPLE_URL);
        JsonMockHttpTransport mockTransport = new JsonMockHttpTransport(failed_task_json);
        afm.mHttpTransport = mockTransport;

        FutureCallback result = new FutureCallback();
        afm.postTask(new Task("TASK_UUID", "TEST_UUID", "wait", 1), result);

        Task resultTask = result.task.get(5000, TimeUnit.MILLISECONDS);
        assertFalse("Task should have failed", result.succeeded);
        assertNotNull("Task should have been returned.", resultTask);
        assertNotNull("Error should be set.", resultTask.task_error);
        assertEquals("Error message should match definition.", resultTask.task_error, "error description");
        assertEquals("Afm should have only made 2 requests.", 2, mockTransport.getRequestsCount());
        checkTaskBasics(resultTask);
    }

    @Test(expected = TimeoutException.class)
    public void testResultTimeout() throws InterruptedException, ExecutionException, TimeoutException {

        Afm afm = Afm.get(HttpTesting.SIMPLE_URL);
        JsonMockHttpTransport mockTransport = new JsonMockHttpTransport(waiting_task_json);
        afm.mHttpTransport = mockTransport;

        FutureCallback result = new FutureCallback();
        afm.postTask(new Task("TASK_UUID", "TEST_UUID", "wait", 1), result);

        try {
            result.task.get(5000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            assertTrue("Afm should have made multiple requests to poll results.", mockTransport.getRequestsCount() > 2);
            throw e;
        }
    }

    @Test
    public void testTaskAbortion() throws InterruptedException, ExecutionException, TimeoutException, BrokenBarrierException {

        final CyclicBarrier barrier = new CyclicBarrier(2);
        final AtomicInteger requestCount = new AtomicInteger(0);

        Afm afm = Afm.get(HttpTesting.SIMPLE_URL);
        afm.mHttpTransport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        if (requestCount.getAndIncrement() > 0) {
                            try {
                                barrier.await(5000, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException e) {
                                throw new IOException(e);
                            } catch (BrokenBarrierException e) {
                                throw new IOException(e);
                            } catch (TimeoutException e) {
                                throw new IOException(e);
                            }
                        }
                        MockLowLevelHttpResponse result = new MockLowLevelHttpResponse();
                        result.setContentType(Json.MEDIA_TYPE);
                        result.setContent(succeeded_task_json);
                        return result;
                    }
                };
            }
        };

        FutureCallback result = new FutureCallback();
        afm.postTask(new Task("TASK_UUID", "TEST_UUID", "wait", 1), result);

        // abort task before Afm gets result, ensured by following barrier
        afm.abortTask("TASK_UUID");
        barrier.await(5000, TimeUnit.MILLISECONDS);

        boolean timeout = false;

        try {
            Task resultTask = result.task.get(5000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            timeout = true;
        }
        assertEquals("Two requests should have made.", 2, requestCount.get());
        assertTrue("result should have timeout.", timeout);
    }

    @Test
    public void testPostIOError() throws InterruptedException, ExecutionException, TimeoutException {

        Afm afm = Afm.get(HttpTesting.SIMPLE_URL);
        afm.mHttpTransport = new MockHttpTransport() {
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

        FutureCallback result = new FutureCallback();
        afm.postTask(new Task("TASK_UUID", "TEST_UUID", "wait", 1), result);

        Task resultTask = result.task.get(5000, TimeUnit.MILLISECONDS);
        assertFalse("Task should have failed", result.succeeded);
        assertNotNull("Task should have been returned.", resultTask);
        assertNotNull("Error should be set.", resultTask.task_error);
    }

    @Test
    public void testPostHttpStatusError() throws InterruptedException, ExecutionException, TimeoutException {

        Afm afm = Afm.get(HttpTesting.SIMPLE_URL);
        afm.mHttpTransport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse result = new MockLowLevelHttpResponse();
                        result.setStatusCode(500);
                        return result;
                    }
                };
            }
        };

        FutureCallback result = new FutureCallback();
        afm.postTask(new Task("TASK_UUID", "TEST_UUID", "wait", 1), result);

        Task resultTask = result.task.get(5000, TimeUnit.MILLISECONDS);
        assertFalse("Task should have failed", result.succeeded);
        assertNotNull("Task should have been returned.", resultTask);
        assertNotNull("Error should be set.", resultTask.task_error);
    }

    @Test
    public void testGetResultTemporaryError() throws InterruptedException, ExecutionException, TimeoutException {

        Afm afm = Afm.get(HttpTesting.SIMPLE_URL);
        afm.mHttpTransport = new MockHttpTransport() {
            int mCount = 0;

            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse result = new MockLowLevelHttpResponse();
                        if (mCount++ > 0 && mCount < 3) {
                            result.setStatusCode(500);
                        } else {
                            result.setContentType(Json.MEDIA_TYPE);
                            result.setContent(succeeded_task_json);
                        }
                        return result;
                    }
                };
            }
        };

        FutureCallback result = new FutureCallback();
        afm.postTask(new Task("TASK_UUID", "TEST_UUID", "wait", 1), result);

        Task resultTask = result.task.get(5000, TimeUnit.MILLISECONDS);
        assertTrue("Task should have succeeded", result.succeeded);
        assertNotNull("Task should have been returned.", resultTask);
        assertNull("Error should not be set.", resultTask.task_error);
    }

    @Test
    public void testBadRequestOnPost() throws InterruptedException, ExecutionException, TimeoutException {

        final AtomicInteger requestCount = new AtomicInteger(0);
        Afm afm = Afm.get(HttpTesting.SIMPLE_URL);
        afm.mHttpTransport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse result = new MockLowLevelHttpResponse();
                        result.setStatusCode(400);
                        requestCount.incrementAndGet();
                        return result;
                    }
                };
            }
        };

        FutureCallback result = new FutureCallback();
        afm.postTask(new Task("TASK_UUID", "TEST_UUID", "wait", 1), result);

        Task resultTask = result.task.get(5000, TimeUnit.MILLISECONDS);
        assertFalse("Task should have failed", result.succeeded);
        assertNotNull("Task should have been returned.", resultTask);
        assertEquals("Exactly one request should have been made", 1, requestCount.get());
    }

    @Test
    public void testBadRequestOnGet() throws InterruptedException, ExecutionException, TimeoutException {

        final AtomicInteger requestCount = new AtomicInteger(0);
        Afm afm = Afm.get(HttpTesting.SIMPLE_URL);
        afm.mHttpTransport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse result = new MockLowLevelHttpResponse();
                        if (requestCount.getAndIncrement() > 0) {
                            result.setStatusCode(400);
                        } else {
                            result.setContentType(Json.MEDIA_TYPE);
                            result.setContent(succeeded_task_json);
                        }
                        return result;
                    }
                };
            }
        };

        FutureCallback result = new FutureCallback();
        afm.postTask(new Task("TASK_UUID", "TEST_UUID", "wait", 1), result);

        Task resultTask = result.task.get(5000, TimeUnit.MILLISECONDS);
        assertFalse("Task should have failed", result.succeeded);
        assertNotNull("Task should have been returned.", resultTask);
        assertEquals("Exactly two request should have been made", 2, requestCount.get());
    }

    @Test
    public void testRequestUrls() throws InterruptedException, ExecutionException, TimeoutException {

        final ArrayList<String> urls = new ArrayList<String>();

        Afm afm = Afm.get(HttpTesting.SIMPLE_URL);
        afm.mHttpTransport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
                urls.add(url);
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        MockLowLevelHttpResponse result = new MockLowLevelHttpResponse();
                        result.setContentType(Json.MEDIA_TYPE);
                        result.setContent(succeeded_task_json);
                        return result;
                    }
                };
            }
        };

        FutureCallback result = new FutureCallback();
        afm.postTask(new Task("TASK_UUID", "TEST_UUID", "wait", 1), result);

        result.task.get(5000, TimeUnit.MILLISECONDS);
        assertEquals("Requests to two urls should have been made", 2, urls.size());

        String postTail = "/task";
        String resultTail = "/task/TASK_UUID";

        assertEquals(urls.get(0).indexOf(postTail), urls.get(0).length() - postTail.length());
        assertEquals(urls.get(1).indexOf(resultTail), urls.get(1).length() - resultTail.length());
    }
}
