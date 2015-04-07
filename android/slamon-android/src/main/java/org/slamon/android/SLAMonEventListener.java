package org.slamon.android;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import org.slamon.Agent;
import org.slamon.AgentEventListener;

import static org.slamon.Agent.ConnectionState.CONNECTED;
import static org.slamon.Agent.ConnectionState.CONNECTING;
import static org.slamon.Agent.ConnectionState.DISCONNECTED;
import static org.slamon.android.SLAMonAndroidAgentService.IntentConstants.*;

/**
 * SLAMon event listener wrapper that changes the events into intent broadcasts
 */
public class SLAMonEventListener implements AgentEventListener {
    LocalBroadcastManager broadcastManager;

    public SLAMonEventListener(LocalBroadcastManager manager) {
        broadcastManager = manager;
    }

    @Override
    public void connectionStateChanged(Agent.ConnectionState state) {
        Intent intent = new Intent(CONNECTION_STATE_CHANGE);
        switch (state) {
            case DISCONNECTED:
                intent = intent.putExtra("status", DISCONNECTED.name());
                break;
            case CONNECTING:
                intent = intent.putExtra("status", CONNECTING.name());
                break;
            case CONNECTED:
                intent = intent.putExtra("status", CONNECTED.name());
                break;
        }
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void fatalError(String message) {
        Intent intent = new Intent(FATAL_ERROR).putExtra("message", message);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void temporaryError(String message) {
        Intent intent = new Intent(TEMPORARY_ERROR).putExtra("message", message);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void taskCompleted() {
        Intent intent = new Intent(TASK_COMPLETED);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void taskError(String message) {
        Intent intent = new Intent(TASK_ERROR).putExtra("message", message);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void taskStarted() {
        Intent intent = new Intent(TASK_STARTED);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void connectionToAfm(long returnTime) {
        Intent intent = new Intent(CONNECTED_TO_AFM).putExtra("returnTime", returnTime);
        broadcastManager.sendBroadcast(intent);
    }
}
