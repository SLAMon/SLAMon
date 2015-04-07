package org.slamon;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

import java.util.Map;

/**
 * A data transfer object to hold SLAMon task data.
 */
public class Task extends GenericJson {
    @Key
    public String task_id;
    @Key
    public String task_type;
    @Key
    public Double task_version;
    @Key
    public Map<String, Object> task_data;
    public Map<String, Object> task_result;
    public String task_error;
    public String task_failed;

    public Task() {
    }

    public Task(String id, String type, Integer version, Map<String, Object> data) {
        this.task_id = id;
        this.task_type = type;
        this.task_version = Double.valueOf(version);
        this.task_data = data;
    }
}

