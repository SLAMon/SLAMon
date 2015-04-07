package org.slamon;

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class AgentTests {

    @Test
    public void testExitOnError() throws AfmCommunicator.TemporaryException, AfmCommunicator.FatalException, TimeoutException {

        AfmCommunicator afm = mock(AfmCommunicator.class);

        when(afm.requestTasks(anyString(), anyString(), anyMap(), anyInt(), anyCollection()))
                .thenThrow(new AfmCommunicator.FatalException("test"));

        Agent agent = new Agent("UUID", "test agent", afm);
        agent.start(1);

        // Agent should exit as soon as it first attempts to contact to AFM,
        // therefore join should complete without timeout.
        agent.join(1000);

        assertFalse(agent.isRunning());
    }

    @Test
    public void testRetryOnTemporaryError() throws AfmCommunicator.TemporaryException, AfmCommunicator.FatalException {

        AfmCommunicator afm = mock(AfmCommunicator.class);

        when(afm.requestTasks(anyString(), anyString(), anyMap(), anyInt(), anyCollection()))
                .thenThrow(new AfmCommunicator.TemporaryException("test"));

        Agent agent = new Agent("UUID", "test agent", afm);

        final ArrayList<Agent.ConnectionState> stateUpdates = new ArrayList<Agent.ConnectionState>();

        agent.addEventListener(new SimpleAgentEventListener() {
            @Override
            public void connectionStateChanged(Agent.ConnectionState state) {
                stateUpdates.add(state);
            }
        });

        agent.start(1);

        // Agent should not exit after catching TemporaryException,
        // therefore join should throw TimeoutException.
        try {
            agent.join(1000);
            assertTrue("Join should have thrown", false);
        } catch (TimeoutException e) {
            assertEquals(2, stateUpdates.size());
            assertEquals(Agent.ConnectionState.CONNECTING, stateUpdates.get(0));
            assertEquals(Agent.ConnectionState.DISCONNECTED, stateUpdates.get(1));
        }
    }

    @Test
    public void testTaskExecution() throws InterruptedException, ExecutionException, TimeoutException {

        final CompletableFuture<Task> taskFuture = new CompletableFuture<Task>();

        Agent.registerHandler(new TaskHandler() {
            @Override
            public Map<String, Object> execute(Map<String, Object> inputParams) throws Exception {
                inputParams.put("output", "output");
                return inputParams;
            }

            @Override
            public String getName() {
                return "test-task";
            }

            @Override
            public int getVersion() {
                return 1;
            }
        });

        AfmCommunicator afm = spy(new AfmCommunicator("url") {
            @Override
            public DateTime requestTasks(String agentId, String agentName, Map<String, Integer> agentCapabilities, int maxTasks, Collection<Task> receivedTasks) throws FatalException, TemporaryException {
                DateTime d = DateTime.now().plusSeconds(1);

                Task task = new Task("id", "test-task", 1, new HashMap<String, Object>());
                receivedTasks.add(task);

                return d;
            }

            @Override
            public void postResults(Task task) throws FatalException, TemporaryException {
                taskFuture.complete(task);
            }
        });

        Agent agent = new Agent("UUID", "test agent", afm);

        agent.start(1);

        Task postTask = taskFuture.get(1000, TimeUnit.MILLISECONDS);
        assertNotNull(postTask);
        assertEquals("output", postTask.task_result.get("output"));

        agent.shutdown(1000);
    }
}
