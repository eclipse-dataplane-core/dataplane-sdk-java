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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataplane.domain.dataflow.DataFlow.State.PREPARED;
import static org.eclipse.dataplane.domain.dataflow.DataFlow.State.STARTED;

public class StreamingPushTest {

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
        var transferType = "FileSystemStreaming-PUSH";
        var processId = UUID.randomUUID().toString();
        var consumerProcessId = "consumer_" + processId;
        var prepareMessage = new DataFlowPrepareMessage("theMessageId", "theParticipantId", "theCounterPartyId",
                "theDataspaceContext", consumerProcessId, "theAgreementId", "theDatasetId", "theCallbackAddress",
                transferType, emptyList(), emptyMap());

        var prepareResponse = controlPlane.consumerPrepare(prepareMessage).statusCode(200).extract().as(DataFlowResponseMessage.class);
        assertThat(prepareResponse.state()).isEqualTo(PREPARED.name());
        assertThat(prepareResponse.dataAddress()).isNotNull();
        var destinationDataAddress = prepareResponse.dataAddress();

        var providerProcessId = "provider_" + processId;
        var startMessage = new DataFlowStartMessage("theMessageId", "theParticipantId", "theCounterPartyId",
                "theDataspaceContext", providerProcessId, "theAgreementId", "theDatasetId", controlPlane.providerCallbackAddress(),
                transferType, destinationDataAddress, emptyList(), emptyMap());
        var startResponse = controlPlane.providerStart(startMessage).statusCode(200).extract().as(DataFlowResponseMessage.class);

        assertThat(startResponse.state()).isEqualTo(STARTED.name());
        assertThat(startResponse.dataAddress()).isNull();

        await().untilAsserted(() -> {
            var folder = Path.of(destinationDataAddress.endpoint());
            assertThat(folder.toFile()).exists().isDirectory().satisfies(destinationFolder -> {
               assertThat(destinationFolder.listFiles()).hasSizeGreaterThan(15);
            });
        });
    }

    private static class ProviderDataPlane {

        private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        private final Dataplane sdk = Dataplane.newInstance()
                .id("provider")
                .onStart(this::onStart)
                .build();

        private Result<DataFlow> onStart(DataFlow dataFlow) {
            var dataAddress = dataFlow.getDataAddress();
            executor.scheduleAtFixedRate(() -> {
                try {
                    var destination = Path.of(dataAddress.endpoint()).resolve(UUID.randomUUID().toString());
                    Files.writeString(destination, UUID.randomUUID().toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, 0, 100L, TimeUnit.MILLISECONDS);

            return Result.success(dataFlow);
        }

        public Object controller() {
            return sdk.controller();
        }
    }

    private static class ConsumerDataPlane {

        private final Dataplane sdk = Dataplane.newInstance()
                .id("consumer")
                .onPrepare(this::onPrepare)
                .onCompleted(this::onCompleted)
                .build();

        private Result<DataFlow> onPrepare(DataFlow dataFlow) {
            try {
                var destinationFolder = Files.createTempDirectory("consumer-dest");
                var dataAddress = new DataAddress("FileSystem", "folder", destinationFolder.toString(), emptyList());

                dataFlow.setDataAddress(dataAddress);

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
    }
}
