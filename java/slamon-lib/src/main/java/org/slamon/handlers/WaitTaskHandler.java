package org.slamon.handlers;

import org.slamon.TaskHandler;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class WaitTaskHandler extends TaskHandler {

    @Override
    public Map<String, Object> execute(Map<String, Object> inputParams) throws Exception {
        Double waitTime = (Double) inputParams.get("time");
        long time = new Date().getTime();
        Thread.sleep(waitTime.intValue() * 1000);
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("waited", new Float(new Date().getTime() - time));
        return ret;
    }

    @Override
    public String getName() {
        return "android-wait";
    }

    @Override
    public int getVersion() {
        return 1;
    }
}
