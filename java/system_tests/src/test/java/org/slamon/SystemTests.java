package org.slamon;

import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.Json;
import com.google.api.client.testing.http.HttpTesting;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

class AFM
{
	private ProcessBuilder processBuilder;
	private Process process;

	public AFM()
	{
		processBuilder = new ProcessBuilder();
		processBuilder.directory(new File("/home/vagrant/SLAMon/"));
	}

	/*
	Sets up AFM environtment (virtualenv + database)
	*/
	public void setup() throws IOException, InterruptedException
	{
		processBuilder.command("/bin/bash", "./test_environment/setup_afm.sh");
		Process setupProcess = processBuilder.start();

		setupProcess.waitFor();
	}

	public void start() throws IOException
	{
		processBuilder.command("/bin/bash", "./test_environment/run_afm.sh");
		process = processBuilder.start();
	}

	public void stop()
	{
		process.destroy();
	}

	public void waitForStartup() throws IOException, InterruptedException
	{
		processBuilder.command("/bin/bash", "./test/environment/wait_for_afm.sh");
		Process waitProcess = processBuilder.start();

		waitProcess.waitFor();
	}
}

class Agent
{
	private ProcessBuilder processBuilder;
	private Process process;

	public Agent()
	{
		processBuilder = new ProcessBuilder();
		processBuilder.directory(new File("/home/vagrant/SLAMon/"));
	}

	/*
	Sets up agent environment (virtualenv)
	*/
	public void setup() throws IOException, InterruptedException
	{
		processBuilder.command("/bin/bash", "./test_environment/setup_agent.sh");
		Process setupProcess = processBuilder.start();

		setupProcess.waitFor();
	}

	public void start() throws IOException
	{
		processBuilder.command("/bin/bash", "./test_environment/run_agent.sh");
		process = processBuilder.start();
	}

	public void stop()
	{
		process.destroy();
	}
}

public class SystemTests
{
	private List<AFM> afms;
	private List<Agent> agents;

	private AFM createAFM() throws IOException, InterruptedException
	{
		AFM afm = new AFM();
		afms.add(afm);

		System.out.println("Setting up AFM environment");
		afm.setup();
		System.out.println("AFM environment ready");

		return afm;
	}

	private Agent createAgent() throws IOException, InterruptedException
	{
		Agent agent = new Agent();
		agents.add(agent);

		System.out.println("Setting up Agent environment");
		agent.setup();
		System.out.println("Agent environment ready");

		return agent;
	}

	private static String waiting_task_json =
			"{" +
					"   \"task_id\": \"TASK_UUID\"," +
					"   \"test_id\": \"TEST_UUID\"," +
					"   \"task_type\": \"wait\"," +
					"   \"task_version\": 1," +
					"   \"task_data\": {" +
					"	   \"time\": 2" +
					"   }" +
					"}";

	private static String failed_task_json =
			"{" +
					"   \"task_id\": \"TASK_UUID\"," +
					"   \"test_id\": \"TEST_UUID\"," +
					"   \"task_type\": \"wait\"," +
					"   \"task_version\": 1," +
					"   \"task_data\": {" +
					"	   \"time\": 2" +
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
					"	   \"time\": 2" +
					"   }," +
					"   \"task_completed\": \"2015-01-17 18:14:04.345757\"," +
					"   \"task_result\": {" +
					"	   \"waited\": 2," +
					"	   \"waited-double\": 2.5," +
					"	   \"string-value\": \"string-value\"" +
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
	public void resetAfm()
	{
		Afm.resetAll();
		afms = new Vector<AFM>();
		agents = new Vector<Agent>();
	}

	@After
	public void resetAFMsAndAgents()
	{
		for (AFM afm : afms)
		{
			afm.stop();
		}

		for (Agent agent : agents)
		{
			agent.stop();
		}
		afms.clear();
		agents.clear();

	}

	@Test
	public void testSuccessResult() throws IOException, InterruptedException, TimeoutException, ExecutionException
	{
		AFM afm = createAFM();
		afm.start();
		afm.waitForStartup();

		Agent agent = createAgent();
		agent.start();

		Task waitTask = new Task("a1635301-cd49-4b76-bf76-e11d089e79f1", "a1635301-cd49-4b76-bf76-e11d089e79f1", "wait", 1);
		waitTask.task_data = new HashMap<String, Object>();
		waitTask.task_data.put("time", 1);

		System.out.println("Posting task to AFM");
		FutureCallback result = new FutureCallback();
		Afm afmClient = Afm.get("http://localhost:8080/");
		afmClient.postTask(waitTask, result);

		Task resultTask = result.task.get(50000, TimeUnit.MILLISECONDS);

		assertNotNull("Task should have been returned", resultTask);
		assertTrue("Task should have succeeded", result.succeeded);
		assertNull("Result should not contain error", resultTask.task_error);

		afm.stop();
		agent.stop();
	}

	@Test
	public void testFailedResult() throws IOException, InterruptedException, TimeoutException, ExecutionException
	{
		AFM afm = createAFM();
		afm.start();
		afm.waitForStartup();

		Agent agent = createAgent();
		agent.start();

		Task waitTask = new Task("a1635301-cd49-4b76-bf76-e11d089e79f1", "a1635301-cd49-4b76-bf76-e11d089e79f1", "wait", 1);
		waitTask.task_data = new HashMap<String, Object>();
		waitTask.task_data.put("not_time", 1);

		System.out.println("Posting task to AFM");
		FutureCallback result = new FutureCallback();
		Afm afmClient = Afm.get("http://localhost:8080/");
		afmClient.postTask(waitTask, result);

		Task resultTask = result.task.get(50000, TimeUnit.MILLISECONDS);

		assertNotNull("Task should have been returned", resultTask);
		assertFalse("Task should not have succeeded", result.succeeded);
		assertNotNull("Result should contain error", resultTask.task_error);

		afm.stop();
		agent.stop();
	}

	@Test(expected = TimeoutException.class)
	public void testUnknownTask() throws IOException, InterruptedException, TimeoutException, ExecutionException
	{
		AFM afm = createAFM();
		afm.start();
		afm.waitForStartup();

		Agent agent = createAgent();
		agent.start();

		Task waitTask = new Task("a1635301-cd49-4b76-bf76-e11d089e79f1", "a1635301-cd49-4b76-bf76-e11d089e79f1", "no_wait", 1);
		waitTask.task_data = new HashMap<String, Object>();
		waitTask.task_data.put("time", 1);

		System.out.println("Posting task to AFM");
		FutureCallback result = new FutureCallback();
		Afm afmClient = Afm.get("http://localhost:8080/");
		afmClient.postTask(waitTask, result);

		Task resultTask = result.task.get(5000, TimeUnit.MILLISECONDS);

		afm.stop();
		agent.stop();
	}

	@Test
	public void testAFMDown() throws InterruptedException, ExecutionException, TimeoutException, IOException
	{
		Agent agent = createAgent();
		agent.start();

		Task waitTask = new Task("a1635301-cd49-4b76-bf76-e11d089e79f1", "a1635301-cd49-4b76-bf76-e11d089e79f1", "wait", 1);
		waitTask.task_data = new HashMap<String, Object>();
		waitTask.task_data.put("time", 1);

		FutureCallback result = new FutureCallback();
		Afm afmClient = Afm.get("http://localhost:8080/");
		afmClient.postTask(waitTask, result);

		Task resultTask = result.task.get(50000, TimeUnit.MILLISECONDS);

		assertFalse("Task should have succeeded", result.succeeded);

		agent.stop();

		// TODO : Check agent logs for errors
	}
}
