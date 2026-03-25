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

package org.eclipse.dataplane.scenario;

import org.eclipse.dataplane.ControlPlane;
import org.eclipse.dataplane.Dataplane;
import org.eclipse.dataplane.HttpServer;
import org.eclipse.dataplane.authorization.TestAuthorization;
import org.eclipse.dataplane.domain.DataAddress;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlow;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStatusMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartMessage;
import org.eclipse.dataplane.domain.registration.ControlPlaneRegistrationMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataplane.authorization.TestAuthorization.TOKEN_GENERATOR;
import static org.eclipse.dataplane.authorization.TestAuthorization.createAuthorizationProfile;
import static org.eclipse.dataplane.domain.dataflow.DataFlow.State.PREPARED;
import static org.eclipse.dataplane.domain.dataflow.DataFlow.State.STARTED;

public class StreamingPushTest {

    private final HttpServer httpServer = new HttpServer();

    private final ControlPlane controlPlane = ControlPlane.newInstance()
            .authorizationTokenGenerator(() -> TOKEN_GENERATOR.apply("control-plane-id"))
            .build();
    private final ConsumerDataPlane consumerDataPlane = new ConsumerDataPlane();
    private final ProviderDataPlane providerDataPlane = new ProviderDataPlane();

    @BeforeEach
    void setUp() {
        httpServer.start();
        controlPlane.initialize(httpServer, "/consumer/data-plane", "/provider/data-plane");

        httpServer.deploy("/consumer/data-plane", consumerDataPlane.controller());
        httpServer.deploy("/provider/data-plane", providerDataPlane.controller());
    }

    @AfterEach
    void tearDown() {
        httpServer.stop();
    }

    @Test
    void shouldPushDataToEndpointPreparedByConsumer() {
        var transferType = "FileSystemStreaming-PUSH";
        var processId = UUID.randomUUID().toString();
        var consumerProcessId = "consumer_" + processId;
        var prepareMessage = new DataFlowPrepareMessage("theMessageId", "theParticipantId", "theCounterPartyId",
                "theDataspaceContext", consumerProcessId, "theAgreementId", "theDatasetId", URI.create("http://callback"),
                transferType, emptyList(), emptyMap());

        var prepareResponse = controlPlane.consumerPrepare(prepareMessage).statusCode(200).extract().as(DataFlowStatusMessage.class);
        assertThat(prepareResponse.state()).isEqualTo(PREPARED.name());
        assertThat(prepareResponse.dataAddress()).isNotNull();
        var destinationDataAddress = prepareResponse.dataAddress();

        var providerProcessId = "provider_" + processId;
        var startMessage = new DataFlowStartMessage("theMessageId", "theParticipantId", "theCounterPartyId",
                "theDataspaceContext", providerProcessId, "theAgreementId", "theDatasetId", controlPlane.providerCallbackAddress(),
                transferType, destinationDataAddress, emptyList(), emptyMap());
        var startResponse = controlPlane.providerStart(startMessage).statusCode(200).extract().as(DataFlowStatusMessage.class);

        assertThat(startResponse.state()).isEqualTo(STARTED.name());
        assertThat(startResponse.dataAddress()).isNull();

        consumerDataPlane.assertDataIsFlowing(consumerProcessId);
    }

    private static class ProviderDataPlane {

        private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
        private final Map<String, ScheduledFuture<?>> flows = new HashMap<>();
        private final Dataplane sdk = Dataplane.newInstance()
                .id("provider")
                .registerAuthorization(new TestAuthorization())
                .onStart(this::onStart)
                .onSuspend(this::stopFlow)
                .build();

        ProviderDataPlane() {
            sdk.registerControlPlane(new ControlPlaneRegistrationMessage("control-plane-id", URI.create("http://localhost:any"), List.of(createAuthorizationProfile("provider"))));
        }

        private Result<DataFlow> onStart(DataFlow dataFlow) {
            var dataAddress = dataFlow.getDataAddress();
            var future = executor.scheduleAtFixedRate(() -> {
                try {
                    var destination = Path.of(dataAddress.endpoint()).resolve(UUID.randomUUID().toString());
                    Files.writeString(destination, UUID.randomUUID().toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, 0, 100L, TimeUnit.MILLISECONDS);

            flows.put(dataFlow.getId(), future);

            return Result.success(dataFlow);
        }

        private Result<DataFlow> stopFlow(DataFlow dataFlow) {
            try {
                flows.get(dataFlow.getId()).cancel(true);
                return Result.success(dataFlow);
            } catch (Exception e) {
                return Result.failure(e);
            }
        }

        public Object controller() {
            return sdk.controller();
        }
    }

    private static class ConsumerDataPlane {

        private final Dataplane sdk = Dataplane.newInstance()
                .id("consumer")
                .registerAuthorization(new TestAuthorization())
                .onPrepare(this::onPrepare)
                .onSuspend(Result::success)
                .onCompleted(this::onCompleted)
                .build();

        private final Map<String, DataAddress> destinations = new HashMap<>();

        ConsumerDataPlane() {
            sdk.registerControlPlane(new ControlPlaneRegistrationMessage("control-plane-id", URI.create("http://localhost:any"), List.of(createAuthorizationProfile("consumer"))));
        }

        private Result<DataFlow> onPrepare(DataFlow dataFlow) {
            try {
                var destinationFolder = Files.createTempDirectory("consumer-dest");
                var dataAddress = new DataAddress("FileSystem", "folder", destinationFolder.toString(), emptyList());

                dataFlow.setDataAddress(dataAddress);
                destinations.put(dataFlow.getId(), dataAddress);

                return Result.success(dataFlow);
            } catch (IOException e) {
                return Result.failure(e);
            }
        }

        private Result<DataFlow> onCompleted(DataFlow dataFlow) {
            var dataAddress = dataFlow.getDataAddress();
            var destination = dataAddress.endpoint();

            try {
                // simulate file forwarding to another service
                var destinationPath = Path.of(destination);
                Files.copy(destinationPath, Files.createTempDirectory("other-service-").resolve(destinationPath.getFileName()));
                return Result.success(dataFlow);
            } catch (IOException e) {
                return Result.failure(e);
            }
        }

        public Object controller() {
            return sdk.controller();
        }

        public void assertDataIsFlowing(String consumerProcessId) {
            var destinationDataAddress = sdk.getById(consumerProcessId).map(DataFlow::getDataAddress)
                    .orElseThrow(f -> new AssertionError("No DataFlow with id %s found".formatted(consumerProcessId)));

            var folder = Path.of(destinationDataAddress.endpoint()).toFile();
            for (var file : Objects.requireNonNull(folder.listFiles())) {
                file.delete();
            }

            await().untilAsserted(() -> {
                assertThat(folder).exists().isDirectory().satisfies(destinationFolder -> {
                    assertThat(destinationFolder.listFiles()).hasSizeGreaterThan(15);
                });
            });

        }

    }
}
