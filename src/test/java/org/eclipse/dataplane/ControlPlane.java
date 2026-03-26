/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.dataplane;

import io.restassured.response.ValidatableResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowResumeMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartedNotificationMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStatusMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowSuspendMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowTerminateMessage;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static jakarta.ws.rs.core.MediaType.WILDCARD;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * This simulates control plane for both consumer and provider.
 * For the sake of simplicity given that there's no DSP wire layer, the flowIds will be the same on provider and on consumer,
 * just prefixed with either "provider_" and "consumer_"
 */
public class ControlPlane {

    private DataplaneClient consumerClient;
    private DataplaneClient providerClient;
    private HttpServer httpServer;
    private Predicate<ContainerRequestContext> authorizationValidation = c -> true;
    private Supplier<Result<String>> authorizationTokenGenerator;

    public static Builder newInstance() {
        return new Builder();
    }

    public void initialize(HttpServer httpServer, String consumerDataPlanePath, String providerDataPlanePath) {
        this.httpServer = httpServer;
        consumerClient = new DataplaneClient("http://localhost:%d%s".formatted(httpServer.port(), consumerDataPlanePath), authorizationTokenGenerator);
        providerClient = new DataplaneClient("http://localhost:%d%s".formatted(httpServer.port(), providerDataPlanePath), authorizationTokenGenerator);

        Predicate<ContainerRequestContext> authorizationProvider = context -> authorizationValidation.test(context);

        httpServer.deploy("/consumer/control-plane", new ControlPlaneController(providerClient, authorizationProvider));
        httpServer.deploy("/provider/control-plane", new ControlPlaneController(consumerClient, authorizationProvider));
    }

    public ValidatableResponse consumerPrepare(DataFlowPrepareMessage prepareMessage) {
        return consumerClient.prepare(prepareMessage);
    }

    public ValidatableResponse consumerStarted(String dataFlowId, DataFlowStartedNotificationMessage startedNotificationMessage) {
        return consumerClient.started(dataFlowId, startedNotificationMessage);
    }

    public ValidatableResponse providerStart(DataFlowStartMessage startMessage) {
        return providerClient.start(startMessage);
    }

    public ValidatableResponse providerSuspend(String flowId, DataFlowSuspendMessage suspendMessage) {
        return providerClient.suspend(flowId, suspendMessage);
    }

    public ValidatableResponse providerResume(String flowId, DataFlowResumeMessage resumeMessage) {
        return providerClient.resume(flowId, resumeMessage);
    }

    public ValidatableResponse providerStatus(String flowId) {
        return providerClient.status(flowId);
    }

    public ValidatableResponse consumerStatus(String flowId) {
        return consumerClient.status(flowId);
    }

    public ValidatableResponse providerTerminate(String dataFlowId, DataFlowTerminateMessage terminateMessage) {
        return providerClient.terminate(dataFlowId, terminateMessage);
    }

    public URI providerCallbackAddress() {
        return URI.create("http://localhost:%d/provider/control-plane".formatted(httpServer.port()));
    }

    public URI consumerCallbackAddress() {
        return URI.create("http://localhost:%d/consumer/control-plane".formatted(httpServer.port()));
    }

    public void setAuthorizationValidation(Predicate<ContainerRequestContext> authorizationValidation) {
        this.authorizationValidation = authorizationValidation;
    }

    @Path("/transfers")
    public static class ControlPlaneController {

        private final ExecutorService executor = Executors.newCachedThreadPool();
        private final DataplaneClient counterPart;
        private final Predicate<ContainerRequestContext> authorizationValidation;

        public ControlPlaneController(DataplaneClient counterPart, Predicate<ContainerRequestContext> authorizationValidation) {
            this.counterPart = counterPart;
            this.authorizationValidation = authorizationValidation;
        }

        @POST
        @Path("/{transferId}/dataflow/prepared")
        @Consumes(WILDCARD)
        public void prepared(@PathParam("transferId") String transferId, @Context ContainerRequestContext context, DataFlowStatusMessage message) {
            if (!authorizationValidation.test(context)) {
                throw new NotAuthorizedException("Not authorized");
            }
            assertThat(message.state()).isEqualTo("PREPARED");
        }

        @POST
        @Path("/{transferId}/dataflow/started")
        @Consumes(WILDCARD)
        public void started(@PathParam("transferId") String transferId, @Context ContainerRequestContext context, DataFlowStatusMessage message) {
            if (!authorizationValidation.test(context)) {
                throw new NotAuthorizedException("Not authorized");
            }
            assertThat(message.state()).isEqualTo("STARTED");
        }

        @POST
        @Path("/{transferId}/dataflow/completed")
        @Consumes(WILDCARD)
        public void completed(@PathParam("transferId") String transferId, @Context ContainerRequestContext context) {
            if (!authorizationValidation.test(context)) {
                throw new NotAuthorizedException("Not authorized");
            }
            executor.submit(() -> {
                var idPart = transferId.split("_")[1];
                counterPart.completed("consumer_" + idPart).statusCode(200);
            });
        }

        @POST
        @Path("/{transferId}/dataflow/errored")
        @Consumes(WILDCARD)
        public void errored(@PathParam("transferId") String transferId, @Context ContainerRequestContext context) {
            if (!authorizationValidation.test(context)) {
                throw new NotAuthorizedException("Not authorized");
            }
            executor.submit(() -> {
                var idPart = transferId.split("_")[1];
                counterPart.terminate("consumer_" + idPart, new DataFlowTerminateMessage("terminated by provider")).statusCode(200);
            });
        }

    }

    public static class Builder {

        private final ControlPlane instance = new ControlPlane();

        private Builder() {

        }

        public ControlPlane build() {
            return instance;
        }

        public Builder authorizationTokenGenerator(Supplier<Result<String>> authorizationTokenGenerator) {
            instance.authorizationTokenGenerator = authorizationTokenGenerator;
            return this;
        }
    }

}
