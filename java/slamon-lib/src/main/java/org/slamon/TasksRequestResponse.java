package org.slamon;

import com.google.api.client.util.Key;

import java.util.Collection;
import java.util.List;

public class TasksRequestResponse {
    @Key
    public List<Task> tasks;
    @Key
    public String return_time;

    TasksRequestResponse() {
    }

    TasksRequestResponse(Collection<Task> tasks, String return_time) {
        this.tasks = (List) tasks;
        this.return_time = return_time;
    }
}