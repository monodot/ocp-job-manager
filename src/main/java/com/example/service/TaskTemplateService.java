package com.example.service;

import com.example.model.Task;
import com.example.model.TaskTemplate;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesResource;
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

        // Process the OpenShift template into a List of Kubernetes objects
        KubernetesList list = client.templates()
                .withName(templateName)
                .process();

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

}
