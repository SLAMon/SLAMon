package org.slamon.android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import dalvik.system.DexFile;
import org.slamon.Agent;
import org.slamon.AgentEventListener;
import org.slamon.TaskHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * SLAMon Android Agent Service that runs in the background and broadcasts events to application
 */
public class SLAMonAndroidAgentService extends Service {
    private String TAG = SLAMonAndroidAgentService.class.getCanonicalName();

    /* Agent configuration */
    private Agent agent;
    private LocalBroadcastManager broadcastManager;
    private AgentEventListener eventListener;
    private String afmURL;
    private String agentName;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Starting SLAMon Android Agent Service");

        // Add task handlers for the Agent
        //addHandlers("org.slamon.agent.android");
        //addHandlers("org.slamon.handlers");

        // Get broadcaster instance
        broadcastManager = LocalBroadcastManager.getInstance(this);
        eventListener = new SLAMonEventListener(broadcastManager);

        // Start agent
        if (afmURL == null || agentName == null) {
            throw new IllegalArgumentException("AFM URL or Agent name was not set");
        }
        startAgent(agentName, afmURL);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        try {
            Log.i(TAG, "Shutting down SLAMon Android Agent Service");
            agent.shutdown(1000);
        } catch (TimeoutException e) {
            Log.w(TAG, "Agent shutdown timeout exceeded!");
        }
        super.onDestroy();
    }

    protected void setup(String afmURL, String agentName) {
        this.afmURL = afmURL;
        this.agentName = agentName;
    }

    protected void setAfmURL(String afmURL) {
        this.afmURL = afmURL;
    }

    protected void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public void setEventListener(AgentEventListener eventListener) {
        this.eventListener = eventListener;
    }

    protected void startAgent(String agentName, String afmURL) {
        // Instantiate agent
        agent = new Agent(
                deviceIdentifier(),
                agentName,
                afmURL);

        // Add SlamonEventListener
        agent.addEventListener(eventListener);

        // Start agent with two concurrent executors
        agent.start(2);
    }

    /**
     * Generate reasonably unique identifier for the device.
     */
    private String deviceIdentifier() {

        ArrayList<byte[]> parts = new ArrayList<byte[]>();
        parts.add(android.os.Build.MANUFACTURER.getBytes());
        parts.add(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID).getBytes());
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (tm != null) {
            parts.add(tm.getDeviceId().getBytes());
        }

        int len = 0;
        for (byte[] part : parts) {
            len += part.length;
        }

        byte[] identifier = new byte[len];
        len = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, identifier, len, part.length);
            len += part.length;
        }

        return UUID.nameUUIDFromBytes(identifier).toString();
    }

    /**
     * Registers all TaskHandlers found in given package inside Android Application's apk file for an Agent to execute.
     * Takes TaskHandlers only from the given package and does not go through subpackages.
     *
     * @param packagePath tells the package that contains TaskHandler subclasses
     */
    protected void addHandlers(String packagePath) {
        try {
            Log.i(TAG, "Adding TaskHandlers from path: " + packagePath);
            DexFile dexFile = new DexFile(getApplication().getPackageCodePath());
            Enumeration<String> entries = dexFile.entries();
            while (entries.hasMoreElements()) {
                String element = entries.nextElement(); // Internal package path e.g. "org.slamon.Agent"
                String elementParent = element.substring(0, element.lastIndexOf('.')); // Class package path
                if (elementParent.equals(packagePath)) {
                    TaskHandler handlerInstance;
                    try {
                        Class<? extends TaskHandler> handler = Class.forName(element).asSubclass(TaskHandler.class);
                        try {
                            handlerInstance = handler.getDeclaredConstructor(Context.class)
                                    .newInstance(getApplicationContext());
                            Agent.registerHandler(handlerInstance);
                            continue;
                        } catch (Exception e) { /* Does not contain constructor with android.Context parameter*/ }
                        handlerInstance = handler.getDeclaredConstructor().newInstance();
                        Agent.registerHandler(handlerInstance);
                    } catch (Exception e) {
                        /* In package path, but can not be instantiated as subclass of TaskHandler */
                    }
                }
            }
        } catch (IOException e) {
            Log.w(TAG, e.getMessage());
        }
    }

    /**
     * Intent action strings
     */
    public class IntentConstants {
        public static final String CONNECTION_STATE_CHANGE = "CONNECTION_STATE_CHANGE";
        public static final String CONNECTED_TO_AFM = "CONNECTED_TO_AFM";
        public static final String FATAL_ERROR = "FATAL_ERROR";
        public static final String TEMPORARY_ERROR = "TEMPORARY_ERROR";
        public static final String TASK_STARTED = "TASK_STARTED";
        public static final String TASK_COMPLETED = "TASK_COMPLETED";
        public static final String TASK_ERROR = "TASK_ERROR";
        public static final String PUSH_NOTIFICATION = "PUSH_NOTIFICATION";
    }


}


