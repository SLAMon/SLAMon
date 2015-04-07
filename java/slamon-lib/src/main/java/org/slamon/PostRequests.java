package org.slamon;

import com.google.api.client.util.Key;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class PostRequest {
}

class TasksRequest extends PostRequest {
    @Key
    public Integer protocol;
    @Key
    public String agent_id;
    @Key
    public String agent_name;
    @Key
    public Map<String, String> agent_location;
    @Key
    public String agent_time;
    @Key
    public Object agent_capabilities;
    @Key
    public int max_tasks;

    // Time formatter to ISO8601 format
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ");

    TasksRequest(int protocol, String agent_id, String agent_name, Map<String, Integer> agent_capabilities,
                 int max_tasks) {
        this.protocol = protocol;
        this.agent_id = agent_id;
        this.agent_name = agent_name;
        this.agent_time = simpleDateFormat.format(new Date().getTime());
        this.agent_capabilities = getAgentCapabilitiesJSON(agent_capabilities);
        this.max_tasks = max_tasks;
    }

    /**
     * Generates a JSON that contains agent's capabilities and their version numbers
     *
     * @param agentCapabilities Map containing information
     * @return JSON containing capability information
     */
    private Map<String, Map<String, Integer>> getAgentCapabilitiesJSON(Map<String, Integer> agentCapabilities) {
        Map<String, Map<String, Integer>> map = new HashMap<String, Map<String, Integer>>();
        for (Map.Entry<String, Integer> entry : agentCapabilities.entrySet()) {
            // Each entry is of format "task-type-1": {"version": 1}
            HashMap<String, Integer> versionMap = new HashMap<String, Integer>();
            versionMap.put("version", entry.getValue());
            map.put(entry.getKey(), versionMap);
        }
        return map;
    }
}

class TaskResultRequest extends PostRequest {
    @Key
    public Integer protocol;
    @Key
    public String task_id;
    @Key
    public Object task_data;
    @Key
    public String task_error;

    public TaskResultRequest(Task task) {
        this.protocol = 1;
        this.task_id = task.task_id;
        if (task.task_result != null) {
            this.task_data = task.task_result;
        } else if (task.task_error != null) {
            this.task_error = task.task_error;
        } else {
            task_data = new HashMap<String, Object>();
        }
    }
}