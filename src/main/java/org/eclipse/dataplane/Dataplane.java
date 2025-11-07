package org.eclipse.dataplane;

import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlow;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowResponseMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartedNotificationMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStatusResponseMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowTerminateMessage;
import org.eclipse.dataplane.logic.OnCompleted;
import org.eclipse.dataplane.logic.OnPrepare;
import org.eclipse.dataplane.logic.OnStart;
import org.eclipse.dataplane.logic.OnStarted;
import org.eclipse.dataplane.logic.OnTerminate;
import org.eclipse.dataplane.port.DataPlaneSignalingApiController;
import org.eclipse.dataplane.port.store.DataFlowStore;
import org.eclipse.dataplane.port.store.InMemoryDataFlowStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.delayedExecutor;

public class Dataplane {

    private final DataFlowStore store = new InMemoryDataFlowStore();
    private String id;
    private OnPrepare onPrepare = _m -> Result.failure(new UnsupportedOperationException("onPrepare is not implemented"));
    private OnStart onStart = _m -> Result.failure(new UnsupportedOperationException("onStart is not implemented"));
    private OnStarted onStarted = _m -> Result.failure(new UnsupportedOperationException("onStarted is not implemented"));;
    private OnCompleted onCompleted = _m -> Result.failure(new UnsupportedOperationException("onCompleted is not implemented"));
    private OnTerminate onTerminate = _m -> Result.failure(new UnsupportedOperationException("onTerminate is not implemented"));
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public static Builder newInstance() {
        return new Builder();
    }

    public DataPlaneSignalingApiController controller() {
        return new DataPlaneSignalingApiController(this);
    }

    public Result<DataFlowResponseMessage> prepare(DataFlowPrepareMessage message) {
        var initialDataFlow = DataFlow.newInstance()
                .id(message.processId())
                .state(DataFlow.State.INITIATING)
                .labels(message.labels())
                .metadata(message.metadata())
                .callbackAddress(message.callbackAddress())
                .transferType(message.transferType())
                .build();

        return onPrepare.action(initialDataFlow)
                .compose(dataFlow -> {
                    if (dataFlow.isInitiating()) {
                        dataFlow.transitionToPrepared();
                    }

                    DataFlowResponseMessage response;
                    if (dataFlow.isPrepared() && dataFlow.isPush()) {
                        response = new DataFlowResponseMessage(id, dataFlow.getDataAddress(), initialDataFlow.getState().name(), null);
                    } else {
                        response = new DataFlowResponseMessage(id, null, initialDataFlow.getState().name(), null);
                    }

                    return store.save(dataFlow).map(it -> response);
                });
    }


    public Result<DataFlowResponseMessage> start(DataFlowStartMessage message) {
        var initialDataFlow = DataFlow.newInstance()
                .id(message.processId())
                .state(DataFlow.State.INITIATING)
                .dataAddress(message.dataAddress())
                .callbackAddress(message.callbackAddress())
                .transferType(message.transferType())
                .build();

        return onStart.action(initialDataFlow)
                .compose(dataFlow -> {
                    if (dataFlow.isInitiating()) {
                        dataFlow.transitionToStarted();
                    }

                    DataFlowResponseMessage response;
                    if (dataFlow.isStarted() && dataFlow.isPull()) {
                        response = new DataFlowResponseMessage(id, dataFlow.getDataAddress(), dataFlow.getState().name(), null);
                    } else {
                        response = new DataFlowResponseMessage(id, null, dataFlow.getState().name(), null);
                    }
                    return store.save(dataFlow).map(it -> response);
                });
    }

    public Result<DataFlowStatusResponseMessage> status(String dataFlowId) {
        return store.findById(dataFlowId)
                .map(f -> new DataFlowStatusResponseMessage(f.getId(), f.getState().name()));
    }

    public Result<Void> terminate(String dataFlowId, DataFlowTerminateMessage message) {
        return store.findById(dataFlowId)
                .map(dataFlow -> {
                    dataFlow.transitionToTerminated(message.reason());
                    return dataFlow;
                })
                .compose(onTerminate::action)
                .compose(store::save)
                .map(it -> null);
    }

    /**
     * Notify the control plane that the data flow has been completed.
     *
     * @param dataFlowId
     */
    public Result<CompletableFuture<Void>> notifyCompleted(String dataFlowId) {
        return store.findById(dataFlowId)
                .map(dataFlow -> transferDataFlowCompleted(dataFlow)
                        .thenApply(r -> {
                            dataFlow.transitionToCompleted();
                            store.save(dataFlow);
                            return null;
                        }));
    }

    private CompletableFuture<HttpResponse<Void>> transferDataFlowCompleted(DataFlow dataFlow) {
        var endpoint = dataFlow.getCallbackAddress() + "/transfers/" + dataFlow.getId() + "/dataflow/completed";

        var request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}")) // TODO DataFlowCompletedMessage not defined
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(r -> {
                    var successful = r.statusCode() >= 200 && r.statusCode() < 300;
                    if (successful) {
                        return CompletableFuture.completedFuture(r);
                    }

                    return CompletableFuture.supplyAsync(() -> transferDataFlowCompleted(dataFlow), delayedExecutor(500, TimeUnit.MILLISECONDS)).thenCompose(Function.identity());
                })
                .exceptionally(CompletableFuture::failedFuture)
                .thenCompose(Function.identity());
    }

    /**
     * Notify the control plane that the data flow failed for some reason
     *
     * @param dataFlow
     * @param throwable
     */
    public void notifyErrored(String dataFlow, Throwable throwable) {
        // TODO: implementation
    }

    public Result<Void> started(String flowId, DataFlowStartedNotificationMessage startedNotificationMessage) {
        return store.findById(flowId)
                .map(dataFlow -> {
                    dataFlow.setDataAddress(startedNotificationMessage.dataAddress());
                    return dataFlow;
                })
                .compose(onStarted::action)
                .compose(dataFlow -> {
                    dataFlow.transitionToStarted();
                    return store.save(dataFlow);
                });
    }

    /**
     * Received notification that the flow has been completed
     *
     * @param flowId
     * @return
     */
    public Result<Void> completed(String flowId) {
        return store.findById(flowId).compose(onCompleted::action)
                .compose(dataFlow -> {
                    dataFlow.transitionToCompleted();
                    return store.save(dataFlow);
                });
    }

    public static class Builder {
        private final Dataplane dataplane = new Dataplane();

        private Builder() {
        }

        public Dataplane build() {
            if (dataplane.id == null) {
                dataplane.id = UUID.randomUUID().toString();
            }
            return dataplane;
        }

        public Builder id(String id) {
            dataplane.id = id;
            return this;
        }

        public Builder onPrepare(OnPrepare onPrepare) {
            dataplane.onPrepare = onPrepare;
            return this;
        }

        public Builder onStart(OnStart onStart) {
            dataplane.onStart = onStart;
            return this;
        }

        public Builder onStarted(OnStarted onStarted) {
            dataplane.onStarted = onStarted;
            return this;
        }

        public Builder onCompleted(OnCompleted onCompleted) {
            dataplane.onCompleted = onCompleted;
            return this;
        }

        public Builder onTerminate(OnTerminate onTerminate) {
            dataplane.onTerminate = onTerminate;
            return this;
        }
    }
}
