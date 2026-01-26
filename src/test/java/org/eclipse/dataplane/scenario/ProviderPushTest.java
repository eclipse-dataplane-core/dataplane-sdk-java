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
import org.eclipse.dataplane.domain.DataAddress;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlow;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowResponseMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStatusResponseMessage;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataplane.domain.dataflow.DataFlow.State.COMPLETED;
import static org.eclipse.dataplane.domain.dataflow.DataFlow.State.PREPARED;
import static org.eclipse.dataplane.domain.dataflow.DataFlow.State.PREPARING;
import static org.eclipse.dataplane.domain.dataflow.DataFlow.State.STARTED;
import static org.eclipse.dataplane.domain.dataflow.DataFlow.State.TERMINATED;

public class ProviderPushTest {

    private final HttpServer httpServer = new HttpServer(21341);

    private final ControlPlane controlPlane = new ControlPlane(httpServer, "/consumer/data-plane", "/provider/data-plane");
    private final ConsumerDataPlane consumerDataPlane = new ConsumerDataPlane();
    private final ProviderDataPlane providerDataPlane = new ProviderDataPlane();

    @BeforeEach
    void setUp() {
        httpServer.start();

        httpServer.deploy("/consumer/data-plane", consumerDataPlane.controller());
        httpServer.deploy("/provider/data-plane", providerDataPlane.controller());
    }

    @AfterEach
    void tearDown() {
        httpServer.stop();
    }

    @Test
    void shouldPushDataToEndpointPreparedByConsumer() {
        var transferType = "FileSystem-PUSH";
        var processId = UUID.randomUUID().toString();
        var consumerProcessId = "consumer_" + processId;
        var prepareMessage = createPrepareMessage(consumerProcessId, controlPlane.consumerCallbackAddress(), transferType);

        var prepareResponse = controlPlane.consumerPrepare(prepareMessage).statusCode(200).extract().as(DataFlowResponseMessage.class);
        assertThat(prepareResponse.state()).isEqualTo(PREPARED.name());
        assertThat(prepareResponse.dataAddress()).isNotNull();
        var destinationDataAddress = prepareResponse.dataAddress();

        var providerProcessId = "provider_" + processId;
        var startMessage = createStartMessage(providerProcessId, controlPlane.providerCallbackAddress(), transferType, destinationDataAddress);
        var startResponse = controlPlane.providerStart(startMessage).statusCode(200).extract().as(DataFlowResponseMessage.class);

        assertThat(startResponse.state()).isEqualTo(STARTED.name());
        assertThat(startResponse.dataAddress()).isNull();

        await().untilAsserted(() -> {
            var path = Path.of(destinationDataAddress.endpoint());
            assertThat(path).exists().content().isEqualTo("hello world");

            var providerStatus = controlPlane.providerStatus(providerProcessId).statusCode(200).extract().as(DataFlowStatusResponseMessage.class);
            assertThat(providerStatus.state()).isEqualTo(COMPLETED.name());

            var consumerStatus = controlPlane.consumerStatus(consumerProcessId).statusCode(200).extract().as(DataFlowStatusResponseMessage.class);
            assertThat(consumerStatus.state()).isEqualTo(COMPLETED.name());
        });
    }

    @Test
    void shouldSendError_whenFlowFails() {
        var transferType = "FileSystem-PUSH";
        var processId = UUID.randomUUID().toString();
        var consumerProcessId = "consumer_" + processId;
        var prepareMessage = createPrepareMessage(consumerProcessId, controlPlane.consumerCallbackAddress(), transferType);

        controlPlane.consumerPrepare(prepareMessage).statusCode(200).extract().as(DataFlowResponseMessage.class);
        var invalidDataAddress = new DataAddress("FileSystem", "", emptyList());

        var providerProcessId = "provider_" + processId;
        var startMessage = createStartMessage(providerProcessId, controlPlane.providerCallbackAddress(), transferType, invalidDataAddress);
        controlPlane.providerStart(startMessage).statusCode(200).extract().as(DataFlowResponseMessage.class);

        await().untilAsserted(() -> {
            var providerStatus = controlPlane.providerStatus(providerProcessId).statusCode(200).extract().as(DataFlowStatusResponseMessage.class);
            assertThat(providerStatus.state()).isEqualTo(TERMINATED.name());

            var consumerStatus = controlPlane.consumerStatus(consumerProcessId).statusCode(200).extract().as(DataFlowStatusResponseMessage.class);
            assertThat(consumerStatus.state()).isEqualTo(TERMINATED.name());
        });
    }

    @Test
    void shouldPermitAsyncPreparation() {
        var transferType = "FileSystemAsync-PUSH";
        var processId = UUID.randomUUID().toString();
        var consumerProcessId = "consumer_" + processId;
        var prepareMessage = createPrepareMessage(consumerProcessId, controlPlane.consumerCallbackAddress(), transferType);

        var prepareResponse = controlPlane.consumerPrepare(prepareMessage).statusCode(202).extract().as(DataFlowResponseMessage.class);
        assertThat(prepareResponse.state()).isEqualTo(PREPARING.name());
        assertThat(prepareResponse.dataAddress()).isNull();

        consumerDataPlane.completePreparation(consumerProcessId);

        assertThat(controlPlane.consumerStatus(consumerProcessId).statusCode(200).extract().as(DataFlowStatusResponseMessage.class).state())
                .isEqualTo(PREPARED.name());
    }

    private @NonNull DataFlowStartMessage createStartMessage(String providerProcessId, String callbackAddress, String transferType, DataAddress destinationDataAddress) {
        return new DataFlowStartMessage("theMessageId", "theParticipantId", "theCounterPartyId",
                "theDataspaceContext", providerProcessId, "theAgreementId", "theDatasetId", callbackAddress,
                transferType, destinationDataAddress, emptyList(), emptyMap());
    }

    private @NonNull DataFlowPrepareMessage createPrepareMessage(String consumerProcessId, String callbackAddress, String transferType) {
        return new DataFlowPrepareMessage("theMessageId", "theParticipantId", "theCounterPartyId",
                "theDataspaceContext", consumerProcessId, "theAgreementId", "theDatasetId", callbackAddress,
                transferType, emptyList(), emptyMap());
    }

    private static class ProviderDataPlane {

        private final ExecutorService executor = Executors.newCachedThreadPool();
        private final Dataplane sdk = Dataplane.newInstance()
                .id("provider")
                .onStart(this::onStart)
                .build();

        private Result<DataFlow> onStart(DataFlow dataFlow) {
            var dataAddress = dataFlow.getDataAddress();
            var future = CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(1000L); // simulates async transfer
                    var destination = Path.of(dataAddress.endpoint());
                    Files.writeString(destination, "hello world");
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }, executor);

            future.whenComplete((completed, throwable) -> {
                if (throwable == null) {
                    sdk.notifyCompleted(dataFlow.getId());
                } else {
                    sdk.notifyErrored(dataFlow.getId(), throwable);
                }
            });

            return Result.success(dataFlow);
        }

        public Object controller() {
            return sdk.controller();
        }
    }

    private static class ConsumerDataPlane {

        private final Dataplane sdk = Dataplane.newInstance()
                .id("thisDataplaneId")
                .onPrepare(this::onPrepare)
                .onCompleted(this::onCompleted)
                .onTerminate(Result::success)
                .build();

        public void completePreparation(String dataFlowId) {
            sdk.getById(dataFlowId)
                    .compose(dataFlow -> sdk.notifyPrepared(dataFlowId, this::prepareDestinationDataAddress))
                    .orElseThrow(f -> new RuntimeException(f.getCause()));
        }

        private Result<DataFlow> onPrepare(DataFlow dataFlow) {
            if (dataFlow.getTransferType().equals("FileSystemAsync-PUSH")) {
                dataFlow.transitionToPreparing();
                return Result.success(dataFlow);
            }

            return prepareDestinationDataAddress(dataFlow);
        }

        private @NonNull Result<DataFlow> prepareDestinationDataAddress(DataFlow dataFlow) {
            try {
                dataFlow.setDataAddress(destinationDataAddress(dataFlow));
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

        private @NonNull DataAddress destinationDataAddress(DataFlow dataFlow) throws IOException {
            var destinationFile = Files.createTempDirectory("consumer-dest").resolve(dataFlow.getId() + "-content");
            return new DataAddress("file", destinationFile.toString(), emptyList());
        }

        public Object controller() {
            return sdk.controller();
        }
    }
}
