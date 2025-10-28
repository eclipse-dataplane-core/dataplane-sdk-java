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
import org.eclipse.dataplane.domain.dataflow.DataFlowTerminateMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataplane.domain.dataflow.DataFlow.State.PREPARED;
import static org.eclipse.dataplane.domain.dataflow.DataFlow.State.STARTED;

public class StreamingPullTest {

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
    void shouldPullDataFromProvider_thenProviderTerminatesIt() {
        var transferType = "FileSystemStreaming-PULL";
        var processId = UUID.randomUUID().toString();
        var consumerProcessId = "consumer_" + processId;
        var prepareMessage = prepareMessage(consumerProcessId, transferType);

        var prepareResponse = controlPlane.consumerPrepare(prepareMessage).statusCode(200).extract().as(DataFlowResponseMessage.class);
        assertThat(prepareResponse.state()).isEqualTo(PREPARED.name());
        assertThat(prepareResponse.dataAddress()).isNull();

        var providerProcessId = "provider_" + processId;
        var startMessage = startMessage(providerProcessId, transferType);
        var startResponse = controlPlane.providerStart(startMessage).statusCode(200).extract().as(DataFlowResponseMessage.class);
        assertThat(startResponse.state()).isEqualTo(STARTED.name());
        assertThat(startResponse.dataAddress()).isNotNull();

        controlPlane.consumerStarted(consumerProcessId, new DataFlowStartedNotificationMessage(startResponse.dataAddress())).statusCode(200);

        await().untilAsserted(() -> {
            assertThat(consumerDataPlane.storage.toFile().listFiles()).hasSizeGreaterThan(20);
        });

        controlPlane.providerTerminate(providerProcessId, new DataFlowTerminateMessage("a good reason")).statusCode(200);

        await().untilAsserted(() -> {
            var filesTransferred = Objects.requireNonNull(consumerDataPlane.storage.toFile().listFiles()).length;
            Thread.sleep(1000L);
            assertThat(consumerDataPlane.storage.toFile().listFiles()).hasSize(filesTransferred);
        });
    }

    private DataFlowPrepareMessage prepareMessage(String consumerProcessId, String transferType) {
        return new DataFlowPrepareMessage("theMessageId", "theParticipantId", "theCounterPartyId",
                "theDataspaceContext", consumerProcessId, "theAgreementId", "theDatasetId", controlPlane.consumerCallbackAddress(),
                transferType, null);
    }

    private DataFlowStartMessage startMessage(String providerProcessId, String transferType) {
        return new DataFlowStartMessage("theMessageId", "theParticipantId", "theCounterPartyId",
                "theDataspaceContext", providerProcessId, "theAgreementId", "theDatasetId", controlPlane.providerCallbackAddress(),
                transferType, null);
    }

    private class ConsumerDataPlane {

        private final Path storage;
        private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

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
                executor.scheduleAtFixedRate(() -> {
                    for (var listFile : Objects.requireNonNull(new File(folder).listFiles())) {
                        try {
                            Files.move(listFile.toPath(), storage.resolve(listFile.getName()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, 0L, 500L, TimeUnit.MILLISECONDS);

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
                .onTerminate(this::onTerminate)
                .build();
        private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        private final Map<String, ScheduledFuture<?>> flows = new HashMap<>();

        public ProviderDataPlane() {
        }

        public Object controller() {
            return sdk.controller();
        }

        private Result<DataFlow> onStart(DataFlow dataFlow) {
            try {
                var destinationDirectory = Files.createTempDirectory(dataFlow.getId());
                var flow = executor.scheduleAtFixedRate(() -> {
                    var path = destinationDirectory.resolve(String.valueOf(UUID.randomUUID().toString()));
                    try {
                        Files.writeString(path, UUID.randomUUID().toString());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, 0L, 100L, TimeUnit.MILLISECONDS);

                flows.put(dataFlow.getId(), flow);

                var dataAddress = new DataAddress("FileSystem", "directory", destinationDirectory.toString(), emptyList());
                dataFlow.setDataAddress(dataAddress);
                dataFlow.transitionToStarted(); // TODO: state change should be done by the service

                return Result.success(dataFlow);
            } catch (IOException e) {
                return Result.failure(e);
            }
        }

        private Result<DataFlow> onTerminate(DataFlow dataFlow) {
            try {
                flows.get(dataFlow.getId()).cancel(true);
                return Result.success(dataFlow);
            } catch (Exception e) {
                return Result.failure(e);
            }
        }
    }
}
