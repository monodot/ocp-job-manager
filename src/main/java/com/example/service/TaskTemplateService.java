package com.example.service;

import com.example.model.Task;
import com.example.model.TaskTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Job;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.api.model.TemplateList;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskTemplateService {

    private static final Logger LOG = LoggerFactory.getLogger(TaskTemplateService.class);

    private final OpenShiftClient client;

    private final TaskTemplateServiceProperties configuration;

    @Autowired
    public TaskTemplateService(OpenShiftClient client,
                               TaskTemplateServiceProperties configuration) {
        this.client = client;
        this.configuration = configuration;
        LOG.debug("Initialised TaskTemplateService; client = {}, configuration Label = {}",
                this.client,
                this.configuration);
    }

    /**
     * Return all OpenShift templates in the current namespace
     */
    public List<TaskTemplate> getTemplateList() {
        LOG.debug("Finding templates with label {}", this.configuration.getLabel());

        List<TaskTemplate> result = new ArrayList<>();

        TemplateList templateList = client.templates()
                .withLabel(this.configuration.getLabel())
                .list();

        LOG.debug("{} templates found", templateList.getItems().size());

        for (Template template : templateList.getItems()) {
            TaskTemplate taskTemplate = new TaskTemplate();

            taskTemplate.setName(template.getMetadata().getName());

            result.add(taskTemplate);
        }

        return result;
    }

    /**
     * Process template
     */
    public List<Task> processTemplate(String templateName) {
        LOG.debug("Processing template {}", templateName);

        // TODO - support non-Pod resources
        // TODO - accept Parameters to the template

        List<Task> result = new ArrayList<>();

        // Process the OpenShift template into a List of Kubernetes objects
        KubernetesList list = client.templates()
                .withName(templateName)
                .process();

        for (KubernetesResource resource : list.getItems()) {
            try {
                LOG.info(SerializationUtils.dumpAsYaml((HasMetadata) resource));
            } catch (JsonProcessingException jpe) {
                LOG.error("Error serialising to YAML");
            }

            if (resource instanceof Pod) {
                Pod pod = (Pod) resource;

                Pod created = client.pods().create(pod);

                Task task = new Task();

                task.setName(created.getMetadata().getName());

                result.add(task);


            }
        }

        return result;


    }

}
