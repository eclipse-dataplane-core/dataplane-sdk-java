package org.eclipse.dataplane;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlow;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowResponseMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartedNotificationMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStatusResponseMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowTerminateMessage;
import org.eclipse.dataplane.domain.registration.DataPlaneRegistrationMessage;
import org.eclipse.dataplane.logic.OnCompleted;
import org.eclipse.dataplane.logic.OnPrepare;
import org.eclipse.dataplane.logic.OnStart;
import org.eclipse.dataplane.logic.OnStarted;
import org.eclipse.dataplane.logic.OnTerminate;
import org.eclipse.dataplane.port.DataPlaneSignalingApiController;
import org.eclipse.dataplane.port.exception.DataFlowNotifyCompletedFailed;
import org.eclipse.dataplane.port.exception.DataplaneNotRegistered;
import org.eclipse.dataplane.port.store.DataFlowStore;
import org.eclipse.dataplane.port.store.InMemoryDataFlowStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Dataplane {

    private final DataFlowStore store = new InMemoryDataFlowStore();
    private String id;
    private String name;
    private String description;
    private String endpoint;
    private final Set<String> transferTypes = new HashSet<>();
    private final Set<String> labels = new HashSet<>();

    private OnPrepare onPrepare = _m -> Result.failure(new UnsupportedOperationException("onPrepare is not implemented"));
    private OnStart onStart = _m -> Result.failure(new UnsupportedOperationException("onStart is not implemented"));
    private OnStarted onStarted = _m -> Result.failure(new UnsupportedOperationException("onStarted is not implemented"));;
    private OnCompleted onCompleted = _m -> Result.failure(new UnsupportedOperationException("onCompleted is not implemented"));
    private OnTerminate onTerminate = _m -> Result.failure(new UnsupportedOperationException("onTerminate is not implemented"));

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    public Result<Void> notifyCompleted(String dataFlowId) {
        return store.findById(dataFlowId)
                .compose(dataFlow -> {
                    var endpoint = dataFlow.getCallbackAddress() + "/transfers/" + dataFlow.getId() + "/dataflow/completed";

                    var request = HttpRequest.newBuilder()
                            .uri(URI.create(endpoint))
                            .header("content-type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("{}")) // TODO DataFlowCompletedMessage not defined
                            .build();

                    var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

                    var successful = response.statusCode() >= 200 && response.statusCode() < 300;
                    if (successful) {
                        dataFlow.transitionToCompleted();
                        return store.save(dataFlow);
                    }

                    return Result.failure(new DataFlowNotifyCompletedFailed(response));
                });
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

    public Result<Void> registerOn(String controlPlaneEndpoint) {

        var message = new DataPlaneRegistrationMessage(id, name, description, endpoint, transferTypes, labels);

        return toJson(message)
                .map(body -> HttpRequest.newBuilder()
                        .uri(URI.create(controlPlaneEndpoint + "/dataplanes/register"))
                        .header("content-type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build()
                )
                .compose(request -> {
                    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        return Result.success();
                    } else {
                        return Result.failure(new DataplaneNotRegistered(response.body()));
                    }
                });
    }

    private Result<String> toJson(DataPlaneRegistrationMessage message) {
        try {
            return Result.success(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            return Result.failure(e);
        }
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

        public Builder name(String name) {
            dataplane.name = name;
            return this;
        }

        public Builder description(String description) {
            dataplane.description = description;
            return this;
        }

        public Builder endpoint(String endpoint) {
            dataplane.endpoint = endpoint;
            return this;
        }

        public Builder transferType(String transferType) {
            dataplane.transferTypes.add(transferType);
            return this;
        }

        public Builder label(String label) {
            dataplane.labels.add(label);
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
