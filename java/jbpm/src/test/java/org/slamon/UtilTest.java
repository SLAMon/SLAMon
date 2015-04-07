package org.slamon;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilTest {

    @Test
    public void testIntValue() {
        assertEquals(Integer.class, Util.convertFromJBPM("1").getClass());
    }

    @Test
    public void testDoubleValue() {
        assertEquals(Double.class, Util.convertFromJBPM("1.5").getClass());
    }

    @Test
    public void testStringValue() {
        assertEquals(String.class, Util.convertFromJBPM("ef9a7896355ac0edae7be8d223da39ba3a76dfd0").getClass());
        assertEquals(String.class, Util.convertFromJBPM("plain string").getClass());
        assertEquals(String.class, Util.convertFromJBPM("\"1.5\"").getClass());
        assertEquals("1.5", Util.convertFromJBPM("\"1.5\""));
    }
}
