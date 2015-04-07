package org.slamon;

import com.google.api.client.util.Key;

import java.util.Map;

/**
 * A data transfer object to hold SLAMon task data.
 */
public class Task {

    @Key
    public String task_id;

    @Key
    public String test_id;

    @Key
    public String task_type;

    @Key
    public String task_completed;

    @Key
    public String task_error;

    @Key
    public String task_failed;

    @Key
    public int task_version;

    @Key
    public Map<String, Object> task_data;

    @Key
    public Map<String, Object> task_result;

    public Task() {
    }

    public Task(String id, String testId, String type, int version) {
        task_id = id;
        test_id = testId;
        task_type = type;
        task_version = version;
    }
}
