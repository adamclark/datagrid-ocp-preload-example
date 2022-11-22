# Overview
This is a simple example application, deployed to OpenShift, to demonstrate Data Grid (Infinispan) cache preloading using an init container.

It is designed for a Data Grid client application with multiple replicas. The cache should only be preloaded once and each replica should not become available without the cache already being preloaded. This is acheived using a combination of an init container and a `StatefulSet`.

Another advantage of this approach is that the init container can have different resources configured (cpu/memory request/limit) compared to the main application container. This is useful when preloading benefits from higher resources compared to the application running in steady state.

This example uses Quarkus but the same approach could be used for any type of application.

# Pre-requisites
1. Create an OpenShift project for this example.
    ```
    oc new-project <project-name>
    ```
1. Using `oc`, log into the OpenShift cluster and use the project created in step 1. This is required so that the image build and deployment use the correct project.
    ```
    oc project <project-name>
    ```
1. Install the Data Grid operator, either for all projects or for the project created in step 1.
 
# Create the Data Grid cluster and caches in OpenShift
```
cd datagrid-openshift
oc apply -f datagrid.yaml
oc apply -f greetings-cache.yaml
oc apply -f preload-control-cache.yaml
```

# Build the init image container in OpenShift
This will compile the init code and start an image build in OpenShift for the init container image used by the app.
```
cd quarkus-datagrid-init
mvn clean package
```

# Build and deploy the application in OpenShift
This will compile the app code, start an image build in OpenShift and deploy the app using a `StatefulSet`.
```
cd quarkus-datagrid-app
mvn clean package -DskipTests
```
A `StatefulSet` is used so that application pods are started sequentially. This is done so that the first pod will start, the init container will see that the cache has not been preloaded and therefore load the cache. Subsequent pods will then start in sequence, the init container of each will see that the cache has already been preloaded and skip loading.

This provides a mechanism to preload the cache once while ensuring that no application containers are available without the cache being preloaded.