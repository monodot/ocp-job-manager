package com.example.service;

import com.example.model.Task;
import com.example.model.TaskTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.JobList;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.api.model.TemplateList;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {

    private static final Logger LOG = LoggerFactory.getLogger(TaskService.class);

    private final OpenShiftClient client;

    @Autowired
    public TaskService(OpenShiftClient client) {
        this.client = client;
    }


    public Task getTask(String name) {
        //TODO

        Task task = new Task();
        task.setName("batch-stuff");
        return task;
    }

    public void stopTask(String name) {
        // TODO to be implemented

    }

    public List<Task> getTasks() {

        PodList podList = client.pods().list();

        List<Task> result = new ArrayList<>();

        for (Pod pod : podList.getItems()) {
            Task task = new Task();
            task.setName(pod.getMetadata().getName());

            result.add(task);
        }

        return result;
    }

    /**
     * Delete a Task
     * @return
     */
    public void deleteJob(String name) {
        // TODO implement this
    }

    public OpenShiftClient getClient() {
        return client;
    }

}
