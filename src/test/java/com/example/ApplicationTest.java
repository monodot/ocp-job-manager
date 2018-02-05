/*
 * Copyright 2005-2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.example;

import com.example.mock.OpenShiftServer;
import com.example.model.Task;
import com.example.model.TaskTemplate;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.openshift.api.model.TemplateBuilder;
import io.fabric8.openshift.api.model.TemplateList;
import io.fabric8.openshift.api.model.TemplateListBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.utils.IOHelpers;
import org.apache.camel.CamelContext;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;

/**
 * Unit tests for the Task Manager REST API.
 *
 * Uses a mock OpenShift/Kubernetes API mockServer, which is configured in
 * {@link com.example.mock.OpenShiftServer}. Underneath the hood this uses
 * `io.fabric8.mockwebserver` to service HTTP requests with test data.
 */
@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class ApplicationTest {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationTest.class);

    private static final String TEST_NAMESPACE = "test";
    private static final String TEMPLATE_LABEL = "taskman";

    /**
     * This will have Spring's RANDOM_PORT value injected into it.
     */
    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private CamelContext camelContext;

    @Rule
    public OpenShiftServer mockServer = new OpenShiftServer();

    /**
     * Wraps the existing OpenShiftClient bean with a spy. This allows us
     * to override the client configuration of the bean, to point to our
     * mock OpenShiftServer.
     */
    @SpyBean
    private OpenShiftClient client;

    /**
     * Ensure that the mock Kubernetes/OpenShift mockServer is returning
     * a TemplateList as expected
     */
    @Test
    @Ignore
    public void testMock() {

        // Configure a single template containing one pod
        mockServer.expect()
                .withPath("/oapi/v1/namespaces/" + TEST_NAMESPACE + "/templates")
                .andReturn(200, new TemplateListBuilder().withItems(
                        new TemplateBuilder().withObjects(
                                new PodBuilder().withNewMetadata().withName("mypod").endMetadata().build()
                        ).build()
                )).once();

        // Grab a client and list templates in the given namespace
        OpenShiftClient client = mockServer.getOpenshiftClient();
        TemplateList templateList = client.templates().inNamespace(TEST_NAMESPACE).list();

        // Make sure the right number of templates was returned
        assertNotNull(templateList);
        assertEquals(1, templateList.getItems().size());
    }

    /**
     * Ensure that the Taskman REST API returns the correct list of Templates
     * @throws IOException
     */
    @Test
    @Ignore
    public void shouldGetTemplateList() throws IOException {

        // Given: the OpenShiftClient mock is configured to talk to our mock
        given(this.client.getConfiguration())
                .willReturn(mockServer.getOpenshiftClient().getConfiguration());
        given(this.client.templates())
                .willReturn(mockServer.getOpenshiftClient().templates());

        // And a single OpenShift template is added to the Mock mockServer
        String json = IOHelpers.readFully(getClass().getResourceAsStream("/template-list.json"));
        mockServer.expect()
                .withPath("/oapi/v1/namespaces/" + TEST_NAMESPACE + "/templates?labelSelector=" + TEMPLATE_LABEL)
                .andReturn(200, json)
                .always();

        // When: we invoke our REST API to get a list of templates
        ResponseEntity<List<TaskTemplate>> response =
                testRestTemplate.exchange("/api/templates",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<TaskTemplate>>() {
                        });
        List<TaskTemplate> templates = response.getBody();

        // Then: we should get a template list containing 1 item
        assertEquals(HttpStatus.OK, response.getStatusCode());
        LOG.info(templates.toString());

    }

    @Test
    @Ignore
    public void processTemplateShouldReturnOneTask() throws IOException {

        // Given: the OpenShiftClient spied bean is configured to talk to our mock
        given(this.client.getConfiguration())
                .willReturn(mockServer.getOpenshiftClient().getConfiguration());
        given(this.client.pods())
                .willReturn(mockServer.getOpenshiftClient().pods());

        // And a single OpenShift template is added to the Mock server
        mockServer.expect().withPath("/oapi/v1/namespaces/test/templates/tmpl1")
                .andReturn(200, new TemplateBuilder().build()).once();
        mockServer.expect().withPath("/oapi/v1/namespaces/test/processedtemplates")
                .andReturn(201, new KubernetesListBuilder().build()).once();

        // When: we invoke our REST API to process a Template
        ResponseEntity<List<Task>> response = testRestTemplate.exchange(
                "/api/templates/mytemplate",
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<List<Task>>() { });

        List<Task> tasks = response.getBody();
        LOG.info("TODO " + tasks.toString());

        // Then: Assert (TODO)

    }


    /**
     * Test that a local mock OpenShiftClient can fetch a list of templates.
     * This is not a functional test of the application itself, just a test
     * class to prove that the client can talk to the mock OpenShiftServer
     */
    @Test
    @Ignore
    public void testOpenShiftClient() throws IOException {

        // Given a mock OpenShift API which returns a template with the given label
        String json = IOHelpers.readFully(getClass().getResourceAsStream("/template-list.json"));
        mockServer.expect()
                .withPath("/oapi/v1/namespaces/" + TEST_NAMESPACE + "/templates?labelSelector=" + TEMPLATE_LABEL)
                .andReturn(200, json)
                .always();

        // When we get a local client and query for templates
        OpenShiftClient client = mockServer.getOpenshiftClient();
        LOG.info("Using OpenShift client URL: " + client.getConfiguration().getMasterUrl());
        TemplateList templateList = client.templates().withLabel(TEMPLATE_LABEL).list();

        // Then we should be returned a template list containing 1 item
        assertEquals(1, templateList.getItems().size());

    }

}