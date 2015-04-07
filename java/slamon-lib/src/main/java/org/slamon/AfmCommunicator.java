package org.slamon;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.Gson;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class that handles AFM communication and
 * provides simple error handling interface for the agent.
 */
public class AfmCommunicator {

    HttpRequestFactory httpRequests = new NetHttpTransport().createRequestFactory();
    private JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
    private String afmServerURL;
    private Logger logger = Logger.getLogger(AfmCommunicator.class.getCanonicalName());

    public AfmCommunicator(String url) {
        this.afmServerURL = url;
        if (!afmServerURL.endsWith("/")) {
            this.afmServerURL += "/";
        }
    }

    /**
     * Connects to the AFM and requests tasks that the agent is capable of performing
     *
     * @param agentId           Agent's uuid
     * @param agentName         Agent's name
     * @param agentCapabilities Map of tasks agent is capable of doing and their version number
     * @param maxTasks          Maximum amount of tasks the agent is capable of taking at once
     * @param receivedTasks     Collection to which received Tasks are appended
     * @return timestamp from AFM when to contact again for tasks
     */
    public DateTime requestTasks(String agentId, String agentName, Map<String, Integer> agentCapabilities,
                             int maxTasks, Collection<Task> receivedTasks) throws FatalException, TemporaryException {
        DateTime result;
        TasksRequestResponse requestResponse;
        TasksRequest tasksRequest = new TasksRequest(1, agentId, agentName, agentCapabilities, maxTasks);
        GenericUrl genericUrl = new GenericUrl(afmServerURL + "tasks/");
        HttpResponse response = sendPostRequest(tasksRequest, genericUrl);

        try {
            // Parse
            String parsed = response.parseAsString();
            requestResponse = new Gson().fromJson(parsed, TasksRequestResponse.class);
            // Extract info
            receivedTasks.addAll(requestResponse.tasks);
            result = new DateTime(requestResponse.return_time);
        } catch (IllegalArgumentException e) {
            throw new FatalException(String.format("Failed to parse response JSON's return time field: %s",
                    e.getMessage()));
        } catch (Exception e) {
            throw new FatalException(
                    String.format("Failed to request tasks successfully: %s", e.getMessage()));
        }
        return result;
    }

    /**
     * Posts task results to the AFM.
     * Handles temporary exceptions and retries the post attempts until succeeds
     *
     * @param task Finished task with results
     * @throws FatalException
     */
    public void postResults(Task task) throws FatalException, TemporaryException {
        TaskResultRequest postRequest = new TaskResultRequest(task);
        GenericUrl genericUrl = new GenericUrl(afmServerURL + "tasks/response");
        String post = new Gson().toJson(postRequest);
        logger.info(String.format("Content of Result post: %s", post));
        sendPostRequest(postRequest, genericUrl);
    }

    /**
     * Single post attempt with given request
     *
     * @param request    Request data
     * @param genericUrl URL for posting tasks
     * @return HTTP Status code of the attempt
     * @throws FatalException
     * @throws TemporaryException
     */
    private HttpResponse sendPostRequest(PostRequest request, GenericUrl genericUrl)
            throws FatalException, TemporaryException {
        HttpResponse httpResponse = null;
        logger.info(String.format("Sending HTTP POST request to %s", genericUrl.toString()));

        try {
            // Create request with JSON content
            HttpRequest httpRequest = httpRequests.buildPostRequest(genericUrl,
                    new JsonHttpContent(jsonFactory, request));
            httpRequest.setParser(new JsonObjectParser(jsonFactory));
            httpResponse = httpRequest.execute();
        } catch (HttpResponseException e) {
            if (e.getStatusCode() >= 400 && e.getStatusCode() < 500) {

                throw new FatalException(String.format("AFM responded with client error status: %s\n%s", e.getStatusMessage(), e.getContent()));
            } else if (e.getStatusCode() >= 500 && e.getStatusCode() < 600) {
                throw new TemporaryException(
                        String.format("AFM responded with server error status: %s", e.getStatusMessage()));
            }
        } catch (IOException e) {
            throw new TemporaryException(
                    String.format("Failed to send the request to AFM: %s", e.getMessage()));
        }

        return httpResponse;
    }

    /**
     * Exception class to indicate that there was a recoverable, temporary error
     * while communicating with the AFM
     */
    public static class TemporaryException extends Exception {
        public TemporaryException(String message) {
            super(message);
        }
    }

    /**
     * Exception class to indicate that there was an unrecoverable error,
     * most likely due to discontinued legacy protocol support
     */
    public static class FatalException extends Exception {
        public FatalException(String message) {
            super(message);
        }
    }
}


