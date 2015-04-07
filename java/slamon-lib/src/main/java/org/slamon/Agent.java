package org.slamon;

import org.joda.time.DateTime;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SLAMon Agent class.
 */
public class Agent implements Runnable {

    final private AfmCommunicator mAfm;
    final private List<AgentEventListener> mEventListeners = new CopyOnWriteArrayList<AgentEventListener>();
    final private Logger logger = Logger.getLogger(Agent.class.getCanonicalName());
    private boolean mRun = false;
    private String mAgentId;
    private String mAgentName;
    private ThreadPoolExecutor mExecutor;
    private Thread mMainThread;
    private ConnectionState mConnectionState = ConnectionState.DISCONNECTED;

    public Agent(String id, String name, String url) {
        mAgentId = id;
        mAgentName = name;
        mAfm = new AfmCommunicator(url);
    }

    Agent(String id, String name, AfmCommunicator afm) {
        mAgentId = id;
        mAgentName = name;
        mAfm = afm;
    }

    public static void registerHandler(TaskHandler taskHandler) {
        TaskHandler.registerHandler(taskHandler);
    }

    public ConnectionState getConnectionState() {
        return mConnectionState;
    }

    private void setConnectionState(ConnectionState newState) {
        if (mConnectionState != newState) {
            mConnectionState = newState;

            for (AgentEventListener listener : mEventListeners) {
                listener.connectionStateChanged(mConnectionState);
            }
        }
    }

    public void addLogHandler(Handler handler) {
        logger.addHandler(handler);
    }

    /**
     * Start the agent in a background thread.
     *
     * @param concurrentExecutors number of task executor threads
     */
    public void start(int concurrentExecutors) {
        mExecutor = new ThreadPoolExecutor(concurrentExecutors, concurrentExecutors, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        mMainThread = new Thread(this);
        mRun = true;
        mMainThread.start();
    }

    /**
     * Shutdown agent background activities.
     *
     * @param timeoutMs timeout in milliseconds to wait for currently running tasks to complete.
     */
    public void shutdown(long timeoutMs) throws TimeoutException {
        mRun = false;
        mExecutor.shutdown();
        mMainThread.interrupt();
        join(timeoutMs);
    }

    void join(long timeoutMs) throws TimeoutException {
        long timeoutTime = new Date().getTime() + timeoutMs;
        while (new Date().getTime() < timeoutTime && mMainThread.isAlive()) {
            try {
                mMainThread.join(timeoutMs);
            } catch (InterruptedException e) {
            }
        }
        if (mMainThread.isAlive()) {
            throw new TimeoutException("Timeout while joining Agent main thread.");
        }
    }

    /**
     * Add new event listener to receive SLAMon Agent events.
     *
     * @param listener to receive events
     */
    public void addEventListener(AgentEventListener listener) {
        mEventListeners.add(listener);
    }

    /**
     * The main loop of the agent.
     */
    @Override
    public void run() {
        try {
            Collection<Task> tasks = new ArrayList<Task>();

            setConnectionState(ConnectionState.CONNECTING);

            while (mRun) {
                // Clear tasks list from previous cycle
                tasks.clear();
                logger.log(Level.INFO, "Cleared tasks from previous execution cycle");

                // Get number of available task executors
                int executorsAvailable = mExecutor.getMaximumPoolSize() - mExecutor.getActiveCount();
                logger.log(Level.INFO, "Available executors: " + executorsAvailable);

                try {
                    // Request for tasks from AFM,
                    // AfmCommunicator should return next poll time as return parameter
                    // and append possible received tasks into tasks-collection.
                    long returnTime = mAfm.requestTasks(
                            mAgentId,
                            mAgentName,
                            TaskHandler.capabilities(),
                            executorsAvailable,
                            tasks).getMillis();

                    logger.log(Level.INFO, "Agent ID: " + mAgentId + ", name: " + mAgentName +
                            " received " + tasks.size() + " tasks.");

                    setConnectionState(ConnectionState.CONNECTED);

                    // Initiate received tasks
                    for (Task task : tasks) {
                        logger.log(Level.INFO, "Starting task " + task.task_type + ":" +
                                task.task_id + " version " + task.task_version);

                        for (AgentEventListener listener : mEventListeners) {
                            listener.taskStarted();
                        }

                        mExecutor.execute(new TaskRunnable(task));
                    }

                    for (AgentEventListener listener : mEventListeners) {
                        listener.connectionToAfm(returnTime);
                    }

                    // Wait for given time
                    Thread.sleep(Math.max(0, returnTime - DateTime.now().getMillis()));
                } catch (AfmCommunicator.TemporaryException e) {
                    logger.log(Level.WARNING, "Encountered recoverable error in Afm communications: " + e.getMessage());
                    for (AgentEventListener listener : mEventListeners) {
                        listener.temporaryError(e.getMessage());
                    }
                    setConnectionState(ConnectionState.DISCONNECTED);

                    // Wait for a minute before contacting AFM again
                    Thread.sleep(60 * 1000);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Encountered unrecoverable error: " + e.getMessage());
            for (AgentEventListener listener : mEventListeners) {
                listener.fatalError(e.getMessage());
            }
            setConnectionState(ConnectionState.DISCONNECTED);
            mRun = false;
            mExecutor.shutdown();
        }
    }

    public boolean isRunning() {
        return mMainThread.isAlive();
    }

    public enum ConnectionState {
        CONNECTING, CONNECTED, DISCONNECTED
    }

    /**
     * Helper class to schedule tasks for execution.
     */
    private class TaskRunnable implements Runnable {

        Task mTask;

        TaskRunnable(Task task) {
            mTask = task;
        }

        @Override
        public void run() {
            try {
                // get relevant task handler
                TaskHandler handler = TaskHandler.getHandler(mTask.task_type, mTask.task_version.intValue());
                logger.log(Level.INFO, "Created TaskHandler " + handler.getName() +
                        ", version: " + handler.getVersion());
                // execute task
                mTask.task_result = handler.execute(mTask.task_data);
                logger.log(Level.INFO, "Excecuted task, result: " + mTask.task_result);
                // send task completion event
                for (AgentEventListener listener : mEventListeners) {
                    listener.taskCompleted();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Task execution failed: " + e.getMessage());
                // set error in task data
                mTask.task_error = e.getMessage();
                // send task error event
                for (AgentEventListener listener : mEventListeners) {
                    listener.taskError(e.getMessage());
                }
            }
            try {
                while (true) {
                    try {
                        mAfm.postResults(mTask);
                        return;
                    } catch (AfmCommunicator.TemporaryException e) {
                        logger.log(Level.WARNING, "Posting task results failed with temporary error: " + e.getMessage());
                    }
                }
            } catch (AfmCommunicator.FatalException e) {
                logger.log(Level.SEVERE, "Posting task results failed with fatal error: " + e.getMessage());
            }
        }
    }
}
