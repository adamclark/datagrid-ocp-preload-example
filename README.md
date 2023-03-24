# Overview
This repo contains two simple example applications, deployed to OpenShift, to demonstrate Data Grid (Infinispan) cache loading during application startup.
The examples address the situation where:
 - The cache(s) needs to be loaded before an application is ready for use.
 - Multiple replicas of the application are started but only one should perform cache loading (and other instances need to wait for loading to complete before becoming ready).

Both examples are designed for a Data Grid client application with multiple replicas. The cache(s) should only be loaded once and each replica should not become available without the cache already being loaded.

The examples uses Quarkus but the same approach could be used for any type of application.

## statefulset-init-loader
The first example `statefulset-init-loader` achieves this using a combination of an init container to control and load the cache and a `StatefulSet`.

A `StatefulSet` is used so that application pods are started sequentially. This is done so that the first pod will start, the init container will see that the cache has not been loaded and therefore load the cache. Subsequent pods will then start in sequence, the init container of each will see that the cache has already been loaded and skip loading.

This provides a mechanism to load the cache once while ensuring that no application containers are available without the cache being loaded.
An advantage of using the init container to load the cache is that the init container can have different resources configured (cpu/memory request/limit) compared to the main application container. This is useful when loading benefits from higher resources compared to the application running in steady state. However, this does increase the complexity of the init container.

## deployment-app-loader
The second example `deployment-app-loader` uses a combination of an init container (to control startup and assignment of cache loading to a pod) and an app container (which loads the cache if assigned to the instance) deployed using a `Deployment`.

In this example all pods will be created together. The init container of each pod will check to see if the cache has been loaded:
- If the cache has been loaded the init container will exit and the app will start normally.
- If the cache has not been loaded the init container will check if another pod is already loading the cache.
  - If another pod is loading the cache the init container will fail so the pod restarts and the checking process starts again. When the cache has been loaded after a restart the app will start normally.
  - If no other pod is loading the cache the init container will assign loading to it's pod and allow the app to start and load the cache.


## How to Run the Examples

The steps below apply to either example. Simply use the relevant directory when building and deploying the init and app images.

If you want to re-run an example or switch to the other example you can use the following command to delete the Data Grid pods so that they are recreated with empty caches:
```
oc delete pod -l app=infinispan-pod
```

### Pre-requisites
Using `oc`, log into the OpenShift cluster and create an OpenShift project for this example.
```
oc new-project <project-name>
```

Use the project created in step 1. This is required so that the image build and deployment use the correct project.
```
oc project <project-name>
```

Install the Data Grid operator, either for all projects or for the project created in step 1.
 
### Create the Data Grid cluster and caches in OpenShift
```
cd datagrid-openshift
oc apply -f datagrid-custom-config.yaml
oc apply -f datagrid.yaml
oc apply -f greetings-cache.yaml
oc apply -f load-control-cache.yaml
```

### Build the init image container in OpenShift
This will compile the init code and start an image build in OpenShift for the init container image used by the app.
```
cd quarkus-datagrid-init
mvn clean package
```

### Build and deploy the application in OpenShift
This will compile the app code, start an image build in OpenShift and deploy the app using a `StatefulSet`.
```
cd quarkus-datagrid-app
mvn clean package -DskipTests
```