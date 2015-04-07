package org.slamon;

import org.junit.Before;
import org.junit.Test;
import org.slamon.handlers.WaitTaskHandler;

import static org.junit.Assert.*;

/**
 * Created by jku on 22.2.2015.
 */
public class TaskHandlerTests {

    @Before
    public void setup() {
        Agent.registerHandler(new WaitTaskHandler());
    }

    @Test
    public void testHandlerRegistration() {
        assertTrue("At lest wait task handler should have been registered!", TaskHandler.capabilities().size() > 0);
    }

    @Test
    public void testHandlerGetter() {
        TaskHandler handler = TaskHandler.getHandler("android-wait", 1);
        assertNotNull(handler);
        assertEquals(handler.getClass(), WaitTaskHandler.class);
    }
}
