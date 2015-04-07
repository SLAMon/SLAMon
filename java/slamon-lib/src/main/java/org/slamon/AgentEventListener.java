package org.slamon;

import static org.slamon.Agent.ConnectionState;

/**
 * SLAMon Event Listener interface
 */
public interface AgentEventListener {
    public void temporaryError(String message);
    public void connectionStateChanged(ConnectionState state);
    public void fatalError(String message);
    public void taskCompleted();
    public void taskError(String message);
    public void taskStarted();

    public void connectionToAfm(long returnTime);
}
