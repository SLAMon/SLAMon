package org.slamon;

/** Helper class implementing all AgentEventListener methods as stubs. */
public class SimpleAgentEventListener implements AgentEventListener {
    @Override
    public void connectionStateChanged(Agent.ConnectionState state) {

    }

    @Override
    public void fatalError(String message) {

    }

    @Override
    public void taskCompleted() {

    }

    @Override
    public void taskError(String message) {

    }

    @Override
    public void taskStarted() {

    }

    @Override
    public void temporaryError(String message) {

    }

    @Override
    public void connectionToAfm(long returnTime) {

    }
}
