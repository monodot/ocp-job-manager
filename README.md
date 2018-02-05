# ocp-task-manager

A management API for creating and monitoring short-lived processes (tasks) using OpenShift pods. Creates Kubernetes resources from OpenShift Templates, and returns statuses via a Swagger-compliant REST API.

The project is implemented using Apache Camel on Spring Boot. It was initially created using:

```
mvn org.apache.maven.plugins:maven-archetype-plugin:2.4:generate \
   -DarchetypeCatalog=https://maven.repository.redhat.com/ga/io/fabric8/archetypes/archetypes-catalog/2.2.195.redhat-000013/archetypes-catalog-2.2.195.redhat-000013-archetype-catalog.xml \
   -DarchetypeGroupId=org.jboss.fuse.fis.archetypes \
   -DarchetypeArtifactId=spring-boot-camel-rest-sql-archetype \
   -DarchetypeVersion=2.2.195.redhat-000013
```

## License

Some classes in `src/test` are taken from the `kubernetes-client` source code, and so they are provided under the Apache License. These files are clearly labelled in the source code.

### Building

The example can be built with

    mvn clean install

### Running the example in OpenShift

It is assumed that:

- OpenShift platform is already running, if not you can find details how to [Install OpenShift at your site](https://docs.openshift.com/container-platform/3.3/install_config/index.html).

- Your system is configured for Fabric8 Maven Workflow, if not you can find a [Get Started Guide](https://access.redhat.com/documentation/en/red-hat-jboss-middleware-for-openshift/3/single/red-hat-jboss-fuse-integration-services-20-for-openshift/)

The example can then be built and deployed using a single goal:

    mvn fabric8:deploy -DskipTests=true
    
**NB:** The Fabric8 Maven Plugin will create a Service Account, `ocp-task-manager`. The app uses this Service Account to query the Kubernetes API. You must grant this Service Account the relevant permissions to view and edit resources in the namespace:

    oc policy add-role-to-user edit -z ocp-task-manager

### Running via an S2I Application Template

Application templates allow you deploy applications to OpenShift by filling out a form in the OpenShift console that allows you to adjust deployment parameters.  This template uses an S2I source build so that it handle building and deploying the application for you.

First, import the Fuse image streams:

    oc create -f https://raw.githubusercontent.com/jboss-fuse/application-templates/GA/fis-image-streams.json

Then, (TODO)

### Accessing the REST service

To get all task templates:

    $ curl http://ocp-task-manager-NAMESPACE.apps.127.0.0.1.nip.io/api/templates
    [{"name":"templatedtask"}]
    
To create resources from a template:

    $ curl -X POST http://ocp-task-manager-NAMESPACE.apps.127.0.0.1.nip.io/api/templates/templatedtask
    {
      "key": "value",
      ...
    }

### Swagger API

The example provides API documentation of the service using Swagger using the _context-path_ `api/api-doc`. You can access the API documentation from your Web browser at <http://ocp-task-manager.example.com/api/api-doc>.

## Tests

The unit tests for this application use the same mock servers defined in `kubernetes-client`.
