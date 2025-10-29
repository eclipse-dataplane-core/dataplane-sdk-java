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
import org.eclipse.dataplane.domain.dataflow.DataFlowStartedNotificationMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataplane.domain.dataflow.DataFlow.State.PREPARED;
import static org.eclipse.dataplane.domain.dataflow.DataFlow.State.STARTED;

public class ConsumerPullTest {

    private final HttpServer httpServer = new HttpServer(21341);
    private final int filesAvailableOnProvider = 13;

    private final ControlPlane controlPlane = new ControlPlane(httpServer, "/consumer/data-plane", "/provider/data-plane");
    private final ConsumerDataPlane consumerDataPlane = new ConsumerDataPlane();
    private final ProviderDataPlane providerDataPlane = new ProviderDataPlane(filesAvailableOnProvider);

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
    void shouldPullDataFromProvider() {
        var transferType = "FileSystem-PULL";
        var processId = UUID.randomUUID().toString();
        var consumerProcessId = "consumer_" + processId;
        var prepareMessage = new DataFlowPrepareMessage("theMessageId", "theParticipantId", "theCounterPartyId",
                "theDataspaceContext", consumerProcessId, "theAgreementId", "theDatasetId", controlPlane.consumerCallbackAddress(),
                transferType, null);

        var prepareResponse = controlPlane.consumerPrepare(prepareMessage).statusCode(200).extract().as(DataFlowResponseMessage.class);
        assertThat(prepareResponse.state()).isEqualTo(PREPARED.name());
        assertThat(prepareResponse.dataAddress()).isNull();

        var providerProcessId = "provider_" + processId;
        var startMessage = new DataFlowStartMessage("theMessageId", "theParticipantId", "theCounterPartyId",
                "theDataspaceContext", providerProcessId, "theAgreementId", "theDatasetId", controlPlane.providerCallbackAddress(),
                transferType, null);
        var startResponse = controlPlane.providerStart(startMessage).statusCode(200).extract().as(DataFlowResponseMessage.class);
        assertThat(startResponse.state()).isEqualTo(STARTED.name());
        assertThat(startResponse.dataAddress()).isNotNull();

        controlPlane.consumerStarted(consumerProcessId, new DataFlowStartedNotificationMessage(startResponse.dataAddress())).statusCode(200);

        await().untilAsserted(() -> {
            assertThat(consumerDataPlane.storage.toFile().listFiles()).hasSize(filesAvailableOnProvider);
        });
    }

    private class ConsumerDataPlane {

        private final Path storage;

        public ConsumerDataPlane() {
            try {
                storage = Files.createTempDirectory("consumer-storage");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private final Dataplane sdk = Dataplane.newInstance()
                .id("consumer")
                .onPrepare(Result::success)
                .onStarted(this::onStarted)
                .build();

        public Object controller() {
            return sdk.controller();
        }

        private Result<DataFlow> onStarted(DataFlow dataFlow) {
            try {
                var folder = dataFlow.getDataAddress().endpoint();
                for (var listFile : Objects.requireNonNull(new File(folder).listFiles())) {
                    Files.copy(listFile.toPath(), storage.resolve(listFile.getName()));
                }

                return Result.success(dataFlow);
            } catch (Exception e) {
                return Result.failure(e);
            }
        }
    }

    private class ProviderDataPlane {

        private final Dataplane sdk = Dataplane.newInstance()
                .id("provider")
                .onStart(this::onStart)
                .build();
        private final int filesToBeCreated;

        public ProviderDataPlane(int fileToBeCreated) {
            this.filesToBeCreated = fileToBeCreated;
        }

        public Object controller() {
            return sdk.controller();
        }

        private Result<DataFlow> onStart(DataFlow dataFlow) {
            try {
                var destinationDirectory = Files.createTempDirectory(dataFlow.getId());
                for (var i = 0; i < filesToBeCreated; i++) {
                    var path = destinationDirectory.resolve(String.valueOf(UUID.randomUUID().toString()));
                    Files.writeString(path, UUID.randomUUID().toString());
                }

                var dataAddress = new DataAddress("FileSystem", "directory", destinationDirectory.toString(), emptyList());
                dataFlow.setDataAddress(dataAddress);

                return Result.success(dataFlow);
            } catch (IOException e) {
                return Result.failure(e);
            }
        }
    }
}
