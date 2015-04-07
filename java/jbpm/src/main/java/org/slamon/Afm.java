package org.slamon;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Agent Fleet Manager client class for posting tasks
 * and receiving asynchronous results. Afm instances
 * are per server singletons, and may be obtained using Afm.get().
 */
public class Afm {

    static final Logger log = Logger.getLogger(Afm.class.getCanonicalName());

    static final HashMap<String, Afm> sAfmServers = new HashMap<String, Afm>();
    static final JsonFactory sJsonFactory = new GsonFactory();

    final ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();
    final HashMap<String, ResultCallback> mTasks = new HashMap<String, ResultCallback>();
    final String mUrl;
    HttpTransport mHttpTransport = new NetHttpTransport();

    private class RequestInitializer implements HttpRequestInitializer {
        @Override
        public void initialize(HttpRequest request) throws IOException {
            request.setParser(new JsonObjectParser(sJsonFactory));
        }
    }

    /**
     * Callback function for receiving task results.
     */
    public interface ResultCallback {

        void succeeded(Task task);

        void failed(Task task);

    }

    private Afm(String url) {
        mUrl = url;
    }

    /**
     * Trigger task callback and drop callback mapping.
     */
    private void completeTask(Task task) {
        ResultCallback callback;
        synchronized (mTasks) {
            callback = mTasks.remove(task.task_id);
        }
        if (callback != null) {
            if (task.task_completed != null) {
                callback.succeeded(task);
            } else {
                callback.failed(task);
            }
        }
    }

    /**
     * Set task error status and trigger callback.
     */
    private void completeTaskWithError(Task task, String error) {
        DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        task.task_failed = fmt.format(new Date());
        task.task_error = error;
        completeTask(task);
    }

    /**
     * Get per server Afm instance.
     *
     * @param url AFM URL
     * @return associated Afm instance
     */
    public static Afm get(String url) {
        Afm instance;
        synchronized (sAfmServers) {
            instance = sAfmServers.get(url);
            if (instance == null) {
                instance = new Afm(url);
                sAfmServers.put(url, instance);
            }
        }
        return instance;
    }

    /**
     * Utility to reset all Afm instances, for testing purposes.
     */
    static void resetAll() {
        synchronized (sAfmServers) {
            for (Map.Entry<String, Afm> e : sAfmServers.entrySet()) {
                Afm instance = e.getValue();
                if (instance != null) {
                    instance.mTasks.clear();
                    instance.mExecutor.shutdownNow();
                    try {
                        instance.mExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
            sAfmServers.clear();
        }
    }

    /**
     * Remove task from result callback mapping
     * to stop result polling and to prevent
     * completion events.
     *
     * @param taskId id of the task to cancel
     */
    public void abortTask(String taskId) {
        log.log(Level.INFO, "Removing task {0} from result map.", taskId);
        synchronized (mTasks) {
            mTasks.remove(taskId);
        }
    }

    /**
     * Post new task for AFM to process. Results are delivered asynchronously
     * to the supplied callback.
     *
     * @param task     task data to post
     * @param callback callback for receiving results
     */
    public void postTask(Task task, ResultCallback callback) {

        try {
            log.log(Level.INFO, "Adding callback for task {0} into result map.", task.task_id);
            synchronized (mTasks) {
                mTasks.put(task.task_id, callback);
            }

            StringBuilder url = new StringBuilder(mUrl);
            if (mUrl.charAt(mUrl.length() - 1) != '/') {
                url.append('/');
            }
            url.append("task");
            GenericUrl postUrl = new GenericUrl(url.toString());

            url.append("/");
            url.append(task.task_id);
            GenericUrl resultUrl = new GenericUrl(url.toString());

            log.log(Level.INFO, "Posting task {0} to {1}...", new Object[]{task.task_id, mUrl});

            HttpRequestFactory factory = mHttpTransport.createRequestFactory(new RequestInitializer());
            HttpRequest request = factory.buildPostRequest(postUrl, new JsonHttpContent(sJsonFactory, task));
            request.execute();
            // according to documentation, .execute() will throw HttpResponseException if
            // http status code >= 300 is received.

            getTask(resultUrl, task);
        } catch (HttpResponseException e) {
            log.log(Level.SEVERE, "Failed to post task {0}: {1}.", new Object[]{task.task_id, e});
            completeTaskWithError(task, e.getMessage());
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to post task {0}: {1}.", new Object[]{task.task_id, e});
            completeTaskWithError(task, e.getMessage());
        }
    }

    /**
     * Schedule result request to be issued for task.
     *
     * @param responseUrl AFM URL
     * @param task        task to request results for
     */
    private void getTask(final GenericUrl responseUrl, final Task task) {
        synchronized (mTasks) {
            if (mTasks.containsKey(task.task_id)) {
                log.log(Level.INFO, "Scheduling result request for task {0}.", task.task_id);
                mExecutor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            log.log(Level.INFO, "Requesting results for task {0}...", task.task_id);
                            HttpRequestFactory factory = mHttpTransport.createRequestFactory(new RequestInitializer());
                            HttpRequest request = factory.buildGetRequest(responseUrl);
                            Task resultTask = request.execute().parseAs(Task.class);

                            if (resultTask.task_completed != null || resultTask.task_failed != null) {
                                log.log(Level.INFO, "Got results for task {0}!", task.task_id);
                                completeTask(resultTask);
                            } else {
                                log.log(Level.INFO, "Results not yet available for task {0}.", task.task_id);
                                // retry
                                getTask(responseUrl, task);
                            }
                        } catch (HttpResponseException e) {
                            if (e.getStatusCode() < 400 || e.getStatusCode() >= 500) {
                                log.log(Level.SEVERE, "Temporary error while getting result for task {0}: {1}", new Object[]{task.task_id, e});
                                // Only retry when status code is not on the
                                // request error range (400)
                                getTask(responseUrl, task);
                            } else {
                                log.log(Level.SEVERE, "Request error while getting result for task {0}: {1}", new Object[]{task.task_id, e});
                                // for request errors (400 series), complete task as failed
                                completeTaskWithError(task, e.getMessage());
                            }
                        } catch (Exception e) {
                            log.log(Level.SEVERE, "Failed get result for task {0}: {1}.", new Object[]{task.task_id, e});
                            // retry
                            getTask(responseUrl, task);
                        }
                    }
                }, 1000, TimeUnit.MILLISECONDS);
            }
        }
    }
}
