# Getting started

In this guide, you will learn the basics of integrating the SDK into your application and how to enable your
application to act as a dataplane.

## Integrating the SDK

All SDK modules are published on Maven Central. To include them into your application, simply add the SDK module(s) as
a dependency. As a minimum, the `dataplane-sdk-core` module is required. This module brings in:
- the domain model including all messages defined in the *Dataplane Signaling* specification
- common logic and message handling
- state handling for the different states of a *data flow*
- in-memory implementations for the stores
- error handling

Example for Gradle in the Kotlin notation:
```kotlin
dependencies {
    implementation("org.eclipse.dataplane-core:dataplane-sdk-core:<version>")
}
```

## Using the SDK in your application

### Configuring the logic

While the core module already provides support for general message handling, the logic that will be executed after a
specific message has been received is specific to your application, data and use case. It therefore needs to be
implemented in order to turn your application into a working dataplane. For this purpose, the SDK provides
interfaces for all Dataplane endpoints defined in the *Dataplane Signaling* specification, that will be called when
the respective endpoint is called. These interfaces are:

| Interface      | Called on          | Task                                                                                                                                |
|----------------|--------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `OnPrepare`    | consumer           | `-PUSH` transfers: generate `DataAddress` to which data will be pushed                                                              |
| `OnStart`      | provider           | `-PUSH` transfers: push data to received `DataAddress`<br/>`-PULL` transfers: generate `DataAddress` from which data will be pulled |
| `OnStarted`    | consumer           | `-PULL` transfers: pull data from received `DataAddress`                                                                            |
| `OnSuspend`    | provider, consumer | suspend a started `DataFlow`                                                                                                        |
| `OnResume`     | provider, consumer | resume a suspended `DataFlow`                                                                                                       |
| `OnCompleted`  | provider, consumer | a `DataFlow` has been completed, i.e. potential associated transfer channels and/or tokens can be closed/revoked                    |
| `OnTerminated` | provider, consumer | a `DataFlow` has been terminated, i.e. potential associated transfer channels and/or tokens can be closed/revoked                   |

For more in-depth information about the communication sequences and which endpoints are called when, check out the
respective chapters in the specification:
- [PUSH messaging sequence](https://github.com/eclipse-dataplane-signaling/dataplane-signaling/blob/main/specifications/signaling.md#pull-protocol-messaging)
- [PULL messaging sequence](https://github.com/eclipse-dataplane-signaling/dataplane-signaling/blob/main/specifications/signaling.md#pull-protocol-messaging)
- [Suspend/Resume sequences](https://github.com/eclipse-dataplane-signaling/dataplane-signaling/blob/main/specifications/signaling.md#data-flow-suspensionresumption)

### Building the dataplane

Once the logic is implemented, you will need to instantiate an instance of the `Dataplane` class. For each of the
logic interfaces listed above, the `Dataplane.Builder` class provides a respective method, in which you can pass your
custom implementation. It will thus be called by the `Dataplane` instance whenever the corresponding action is called.

Additionally, the `Dataplane.Builder` class provides methods for the following properties:
- `id`: the unique identifier of the dataplane instance
- `endpoint`: the URL under which the Dataplane API is reachable, will be your application's base URL and path plus `/v1/dataflows`
- `transferType`: a [transfer type](https://github.com/eclipse-dataplane-signaling/dataplane-signaling/blob/main/specifications/signaling.md#data-transfer-types) supported by the dataplane (multiple transfer types can be added)
- `label`: a label for the dataplane instance which can be used by control planes to filter for specific dataplanes (multiple labels can be added)
- `authorization`: defines the authorization used between control and dataplane, for more information see [Authorizations](#authorizations)

*There is one additional builder method called `stores()`, which will be detailed later in this guide. When not setting
any stores specifically, both `DataFlow` and `ControlPlane` information will be stored in-memory.*

```java
var dataplane = Dataplane.newInstance()
        .id("<your-dataplane-id>")
        .endpoint(format("%s/v1/dataflows", "<application-base-url-and-path>"))
        .transferType("<transfer-type1>")
        .transferType("<transfer-type2>")
        .label("<label1>")
        .onPrepare(new MyOnPrepareImplementation())
        .onStart(new MyOnStartImplementation())
        .onStarted(new MyOnStartedImplementation())
        .onStarted(new MyOnSuspendImplementation())
        .onStarted(new MyOnResumeImplementation())
        .onCompleted(new MyOnCompletedImplementation())
        .onTerminate(new MyOnTerminatedImplementation())
        .build();
```

With this, you have the basic setup of your dataplane ready. When calling any of the actions on the `Dataplane`
instance, it will now use your logic implementations to handle the respective actions. The only thing now missing is
the actual API, for which we need a controller, which is detailed in
[Setting up the controllers](#setting-up-the-controllers).

### Authorizations

Usually, communication between control and dataplane will use authentication. Thus, the dataplane needs to be able
to add `Authorization` headers to its own requests as well as validate incoming `Authorization` headers sent by the
control plane. For this purpose, the SDK provides an `Authorization` interface, which defines methods for
creating an `Authorization` header and for extracting the ID of the control plane from an incoming `Authorization`
header.

Out-of-the-box, the SDK provides an
[OAuth2 implementation](../dataplane-sdk-core/src/main/java/org/eclipse/dataplane/domain/registration/Oauth2ClientCredentialsAuthorization.java).
Depending on your environment, you can either use this one or create your own implementation of `Authorization` based
on the protocol/technology of your choice.

**Note, that the `Authorization`'s `type()` needs to match the `type` of the `AuthorizationProfile` sent in the
[registrations messages](https://github.com/eclipse-dataplane-signaling/dataplane-signaling/blob/main/specifications/signaling.md#registration).** 

### Setting up the controllers

For setting up the controllers for the Dataplane API, you have two different options:

The SDK out-of-the-box comes with controllers based on the `Jakarta-EE` framework. For details on how to use this
implementation, check out the chapter
[Running a dataplane with Jersey & Postgres](#running-a-dataplane-with-jersey--postgres).

Alternatively, if your application is using a different web technology like e.g. Springboot, you need to implement
your own controllers. Details on how to do this can be found in the chapter
[Implementing support for a different web technology](#implementing-support-for-a-different-web-technology).

### Registering your dataplane with a control plane

Once your dataplane is ready to be used, you need to register it with a control plane, so that it is available for the
control plane during a data transfer. You can either make the corresponding request manually by following the
[endpoint specification](https://github.com/eclipse-dataplane-signaling/dataplane-signaling/blob/main/specifications/signaling.md#data-plane-registration),
or you can use the SDK's built-in registration. To do so, add the following call to your code after creating the
`Dataplane` instance:

```java
var result = dataplane.registerOn("<control-plane-url>");
if (result.failed()) {
    // ... handle registration error
}
```

Using the SDK's `Result` and its `succeeded()`/`failed()` methods, you can check whether the registration was successful
or not and handle potential errors accordingly. In case a `Result` is failed, you can call its `getException()` method
to receive details about the error.

## Running a dataplane with Jersey & Postgres

Out-of-the-box, the SDK comes with a controller based on the `Jakarta-EE` framework and a persistence layer based on
`PostgreSQL`. To use them, add the modules `dataplane-sdk-jakarta-ee` and `dataplane-sdk-postgresql` respectively
to your dependencies. To use the controller, you will also need an implementation of the `Jakarta-EE` framework, like
`Jersey`, and of course a web server. In the SDK's integration tests, `Jetty` is used, but other web servers should
work as well, as long as they support `Jakarta-EE`.

Example dependencies for Gradle in the Kotlin notation:
```kotlin
dependencies {
    implementation("org.eclipse.dataplane-core:dataplane-sdk-core:<version>")
    implementation("org.eclipse.dataplane-core:dataplane-sdk-jakarta-ee:<version>")
    implementation("org.eclipse.dataplane-core:dataplane-sdk-postgresql:<version>")

    implementation("org.glassfish.jersey.inject:jersey-hk2:<version>")
    implementation("org.glassfish.jersey.media:jersey-media-json-jackson:<version>")
    implementation("org.glassfish.jersey.containers:jersey-container-servlet:<version>")
    implementation("org.eclipse.jetty.ee10:jetty-ee10-servlet:<version>")
    implementation("org.eclipse.jetty:jetty-server:<version>")
    
    implementation("org.postgresql:postgresql:<version>") 
}
```

Note, that the SDK has been tested only with the versions found in [libs.versions.toml](../gradle/libs.versions.toml).
Other versions may work, too, but are not guaranteed to be compatible.

### Making the controllers available

The dataplane API comprises 2 different APIs, one for managing data flows, that will be called during an ongoing
transfer, and one for registering and unregistering control planes. For both, a controller implementation is available.
You can instantiate the controllers as follows, passing your `Dataplane` instance to them:

```java
var controller = new DataPlaneSignalingApiController(dataplane);
var registratioController = new DataPlaneRegistrationApiController(dataplane);
```

Now, when any of the controllers' endpoints are called, they will call the corresponding method on your `Dataplane`
instance.

The only thing left to do now is registering your controllers and making them available on your web server. The
following is a basic example of how to do this from scratch using `Jetty` and `Jersey`. Of course, your application
may already have a web server in place and configured. In that case, you can simply register the controllers with
your existing web server.

> Note, that the following code snippet is just an example to help you get started quickly. In a production environment,
> additional configuration and setup may be needed.

```java
var server = new Server();
var connector = new ServerConnector(server);
connector.setPort(8080);
server.setConnectors(new Connector[]{connector});
server.setHandler(new ServletContextHandler(NO_SESSIONS));

var resourceConfig = new ResourceConfig();
resourceConfig.registerClasses(controller.getClass());
resourceConfig.registerClasses(registratioController.getClass());
resourceConfig.registerInstances(new AbstractBinder() {
    @Override
    protected void configure() {
        List.of(controller, registrationController).forEach(c -> bind(c).to((Class<? super Object>) c.getClass()));
    }
});
var servlet = new ServletContainer(resourceConfig);

var servletHolder = new ServletHolder(Source.EMBEDDED);
servletHolder.setServlet(servlet);
servletContextHandler.getServletHandler().addServletWithMapping(servletHolder, <application-base-path> + "/*");

server.start();
```

Now, when starting your application, you should be able to reach the
[endpoints of the Dataplane API](https://github.com/eclipse-dataplane-signaling/dataplane-signaling/blob/main/specifications/signaling.md#data-plane-endpoint)
under `<application-base-url>(:<port>)/<application-base-path>/v1/dataflows/<endpoint>`.

### Configuring the stores

The SDK uses two stores, one for storing `DataFlow` and one for storing `ControlPlane` information. By default, both
stores run in-memory, but the SDK provides PostgreSQL-based implementations. Before instantiating these stores, you will
need a Jackson `ObjectMapper` and a `DataSource` (this can either be a simple or a pooled data source). With these in
place, you can instantiate the stores as follows:

```java
var dataFlowStore = new PostgresDataFlowStore(objectMapper, dataSource);
var controlPlaneStore = new PostgresControlPlaneStore(objectMapper, dataSource);
```

Afterwards, wrap both of them into a `Stores` instance and pass it to the `Dataplane.Builder`:

```java
var stores = new Stores(dataFlowStore, controlPlaneStore);
var dataplane = Dataplane.newInstance()
        // ... other builder methods
        .stores(stores)
        .build();
```

With that, you're done. Your dataplane will now store all `DataFlow` and `ControlPlane` information in the configured
PostgreSQL database.

> For initializing your database, you can take a look at the schemas for
> [data flows](../dataplane-sdk-postgresql/src/main/resources/sql/data_flow_schema.sql) and
> [control planes](../dataplane-sdk-postgresql/src/main/resources/sql/control_plane_schema.sql).

## Implementing support for a different web technology

If your application is using a different web technology, e.g. as part of a Springboot application, you may want to
implement your own controllers based on the respective framework for easier integration. When doing so, you can use the
existing, [`Jakarta-EE`-based controllers](../dataplane-sdk-jakarta-ee/src/main/java/org/eclipse/dataplane/port) for
reference. The structure of the endpoints should match the one from the existing controllers, i.e. HTTP method, paths,
headers and request/response bodies should be the same to ensure they are compatible with the
[API specification](https://github.com/eclipse-dataplane-signaling/dataplane-signaling/blob/main/specifications/signaling.md#data-plane-endpoint).
Internally, the controllers should also match the existing controllers' behaviours, using the `Dataplane` instance
to trigger the respective actions.

Keep in mind, that the `Dataplane` may throw exceptions in case of errors. Therefore, you should expect specific
exception types and handle them accordingly by mapping them to respective HTTP status codes. For reference, take a
look at the existing
[ExceptionMapper](../dataplane-sdk-jakarta-ee/src/main/java/org/eclipse/dataplane/port/ExceptionMapper.java).

Once the implementations are done and the controllers are made available on your web server, you can omit the
`dataplane-sdk-jakarta-ee` module from your dependencies.

## Implementing support for a different persistence layer

To use a different persistence layer than PostgreSQL, which could either be a different SQL-based database or an
entirely different storage backend, you will need to create your own implementations of
[DataFlowStore](../dataplane-sdk-core/src/main/java/org/eclipse/dataplane/port/store/DataFlowStore.java)
and
[ControlPlaneStore](../dataplane-sdk-core/src/main/java/org/eclipse/dataplane/port/store/ControlPlaneStore.java) based
on the respective storage technology.

Once the implementations are done, you can wrap them in a `Stores` instance and pass them to your `Dataplane` via the
builder:

```java
var stores = new Stores(new MyCustomDataFlowStore(), new MyCustomControlPlaneStore());
var dataplane = Dataplane.newInstance()
        // ... other builder methods
        .stores(stores)
        .build();
```

You can then omit the `dataplane-sdk-postgresql` module from your dependencies.
