package org.slamon;

import java.math.BigDecimal;
import java.util.UUID;

import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.WorkItem;

import com.google.gson.Gson;

public class Util {

    public static String itemId(RuntimeEngine engine, WorkItem workItem) {
        org.kie.api.definition.process.Process process = engine.getKieSession().getProcessInstance(workItem.getProcessInstanceId()).getProcess();
        String data = process.getId() + process.getPackageName() + process.getVersion() + workItem.getProcessInstanceId() + workItem.getId();
        return UUID.nameUUIDFromBytes(data.getBytes()).toString();
    }

    public static String processId(RuntimeEngine engine, WorkItem workItem) {
        org.kie.api.definition.process.Process process = engine.getKieSession().getProcessInstance(workItem.getProcessInstanceId()).getProcess();
        String data = process.getId() + process.getPackageName() + process.getVersion() + workItem.getProcessInstanceId();
        return UUID.nameUUIDFromBytes(data.getBytes()).toString();
    }

    /**
     * A helper to convert result variables for jBPM.
     * Afm will return integer and double values as BigDecimal, this
     * helper will convert those to Integer and Double for jBPM compatibility.
     *
     * @param value The input value, String or BigDecimal
     * @return Value jBPM output value, Integer, Double or String
     */
    public static Object convertToJBPM(Object value) {

        if (BigDecimal.class.isInstance(value)) {
            BigDecimal v = (BigDecimal) value;
            if (v.scale() == 0) {
                return v.intValue();
            }
            return v.doubleValue();
        }

        return value;
    }

    /**
     * A helper to convert (or guess conversion) string value to
     * integer or double when appropriate. This is a workaround for jBPM limitation
     * causing all input variables passed in as a java.lang.String.
     *
     * @param value Input value
     * @return Value encapsulated in guessed container (Integer, Double or String)
     */
    public static Object convertFromJBPM(Object value) {
        if (String.class.isInstance(value)) {
            try {
                return Integer.valueOf((String) value);
            } catch (Exception e) {
                try {
                    return Double.valueOf((String) value);
                } catch (Exception e2) {
                    try {
                        Gson gson = new Gson();
                        return gson.fromJson((String) value, String.class);
                    } catch (Exception e3) {
                        return value;
                    }
                }
            }
        }
        return value;
    }
}
