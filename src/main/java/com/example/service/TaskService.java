package com.example.service;

import com.example.model.Task;
import com.example.model.TaskTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.extensions.JobList;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.api.model.TemplateList;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix="jobmanager")
public class TaskService {

    private static final Logger LOG = LoggerFactory.getLogger(TaskService.class);

    private final OpenShiftClient client;

    private String templateLabel;

    @Autowired
    public TaskService(OpenShiftClient client) {
        this.client = client;
    }


    public Task getJob(String name) {
        //TODO

        Task job = new Task();
        job.setName("batch-stuff");
        return job;
    }

    public void stopJob(String name) {
        // TODO to be implemented

        io.fabric8.kubernetes.api.model.extensions.Job myJob = client.extensions().jobs().withName("hiya").get();

        // TODO how to stop a job here?
    }

    public List<Task> getJobs() {
        JobList jobList = client.extensions().jobs().list();

        List<Task> result = new ArrayList<>();

        for (io.fabric8.kubernetes.api.model.extensions.Job kubernetesJob : jobList.getItems()) {
            Task job = new Task();
            job.setName(kubernetesJob.getMetadata().getName());
            result.add(job);
        }

        return result;
    }

    /**
     * Process the given template and create resources
     */
    public KubernetesList createJob(String templateName) throws JsonProcessingException {
        // TODO should also accept parameters
        // TODO support Kubernetes 3.4+
        // TODO support creating other objects in the Template

        KubernetesList list = client.templates().withName(templateName).process();

        for (KubernetesResource resource : list.getItems()) {
            LOG.info(SerializationUtils.dumpAsYaml((HasMetadata) resource));

            if (resource instanceof io.fabric8.kubernetes.api.model.extensions.Job) {
                io.fabric8.kubernetes.api.model.extensions.Job job =
                        (io.fabric8.kubernetes.api.model.extensions.Job) resource;

                // Force this for compatibility with Kubernetes version 3.3
                // kubernetes-client will use API version for Jobs = 'batch/v1'
                // But Kubernetes 3.3 expects API version for Jobs = 'extensions/v1beta1'
                // So we manually force the API version
                job.setApiVersion("extensions/v1beta1");

                // Additionally, the autoSelector property needs to be set
                // otherwise Kubernetes complains that `selector` and `labels` are null
                // See: https://github.com/kubernetes/kubernetes/issues/23599
                job.getSpec().setAutoSelector(true);

                LOG.info(SerializationUtils.dumpAsYaml((HasMetadata) resource));

                client.extensions().jobs().create(job);
            }
        }

        return null;
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

    public String getTemplateLabel() {
        return templateLabel;
    }

    public void setTemplateLabel(String templateLabel) {
        this.templateLabel = templateLabel;
    }
}
