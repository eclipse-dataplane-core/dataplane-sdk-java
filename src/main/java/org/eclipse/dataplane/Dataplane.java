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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - data flow properties
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - introduce DataFlowStatusMessage
 *
 */

package org.eclipse.dataplane;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.controlplane.ControlPlane;
import org.eclipse.dataplane.domain.dataflow.DataFlow;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowResumeMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartedNotificationMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStatusMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStatusResponseMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowSuspendMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowTerminateMessage;
import org.eclipse.dataplane.domain.registration.Authorization;
import org.eclipse.dataplane.domain.registration.ControlPlaneRegistrationMessage;
import org.eclipse.dataplane.domain.registration.DataPlaneRegistrationMessage;
import org.eclipse.dataplane.logic.OnCompleted;
import org.eclipse.dataplane.logic.OnPrepare;
import org.eclipse.dataplane.logic.OnResume;
import org.eclipse.dataplane.logic.OnStart;
import org.eclipse.dataplane.logic.OnStarted;
import org.eclipse.dataplane.logic.OnSuspend;
import org.eclipse.dataplane.logic.OnTerminate;
import org.eclipse.dataplane.port.DataPlaneRegistrationApiController;
import org.eclipse.dataplane.port.DataPlaneSignalingApiController;
import org.eclipse.dataplane.port.exception.AuthorizationNotSupported;
import org.eclipse.dataplane.port.exception.ControlPlaneNotRegistered;
import org.eclipse.dataplane.port.exception.DataFlowNotifyControlPlaneFailed;
import org.eclipse.dataplane.port.exception.DataplaneNotRegistered;
import org.eclipse.dataplane.port.exception.ResourceNotFoundException;
import org.eclipse.dataplane.port.store.ControlPlaneStore;
import org.eclipse.dataplane.port.store.DataFlowStore;
import org.eclipse.dataplane.port.store.InMemoryControlPlaneStore;
import org.eclipse.dataplane.port.store.InMemoryDataFlowStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static java.util.Collections.emptyMap;

public class Dataplane {

    private final ObjectMapper objectMapper = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final DataFlowStore dataFlowStore = new InMemoryDataFlowStore(objectMapper);
    private final ControlPlaneStore controlPlaneStore = new InMemoryControlPlaneStore(objectMapper);
    private String id;
    private URI endpoint;
    private final Set<String> transferTypes = new HashSet<>();
    private final Set<String> labels = new HashSet<>();

    private OnPrepare onPrepare = dataFlow -> Result.failure(new UnsupportedOperationException("onPrepare is not implemented"));
    private OnStart onStart = dataFlow -> Result.failure(new UnsupportedOperationException("onStart is not implemented"));
    private OnTerminate onTerminate = dataFlow -> Result.failure(new UnsupportedOperationException("onTerminate is not implemented"));
    private OnSuspend onSuspend = dataFlow -> Result.failure(new UnsupportedOperationException("onSuspend is not implemented"));
    private OnResume onResume = dataFlow -> Result.failure(new UnsupportedOperationException("onResume is not implemented"));
    private OnStarted onStarted = dataFlow -> Result.failure(new UnsupportedOperationException("onStarted is not implemented"));
    private OnCompleted onCompleted = dataFlow -> Result.failure(new UnsupportedOperationException("onCompleted is not implemented"));

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Map<String, Authorization> authorizations = new HashMap<>();

    public static Builder newInstance() {
        return new Builder();
    }

    public DataPlaneSignalingApiController controller() {
        return new DataPlaneSignalingApiController(this, authorizations);
    }

    public DataPlaneRegistrationApiController registrationController() {
        return new DataPlaneRegistrationApiController(this);
    }

    public Result<DataFlow> getById(String dataFlowId) {
        return dataFlowStore.findById(dataFlowId);
    }

    public Result<Void> save(DataFlow dataFlow) {
        return dataFlowStore.save(dataFlow);
    }

    public Result<DataFlowStatusResponseMessage> status(String dataFlowId) {
        return dataFlowStore.findById(dataFlowId)
                .map(f -> new DataFlowStatusResponseMessage(f.getId(), f.getState().name()));
    }

    private Result<Void> checkControlPlane(String controlplaneId) {
        if (controlPlaneStore.exists(controlplaneId)) {
            return Result.success();
        }
        return Result.failure(new ControlPlaneNotRegistered(controlplaneId));
    }

    public Result<DataFlowStatusMessage> prepare(String controlplaneId, DataFlowPrepareMessage message) {
        var initialDataFlow = DataFlow.newInstance()
                .id(message.processId())
                .state(DataFlow.State.INITIATING)
                .labels(message.labels())
                .metadata(message.metadata())
                .callbackAddress(message.callbackAddress())
                .transferType(message.transferType())
                .datasetId(message.datasetId())
                .agreementId(message.agreementId())
                .participantId(message.participantId())
                .counterPartyId(message.counterPartyId())
                .dataspaceContext(message.dataspaceContext())
                .controlplaneId(controlplaneId)
                .type(DataFlow.Type.CONSUMER)
                .build();

        return checkControlPlane(controlplaneId)
                .compose(v -> onPrepare.action(initialDataFlow))
                .compose(dataFlow -> {
                    if (dataFlow.isInitiating()) {
                        dataFlow.transitionToPrepared();
                    }

                    DataFlowStatusMessage response;
                    if (dataFlow.isPrepared() && dataFlow.isPush()) {
                        response = new DataFlowStatusMessage(id, dataFlow.getId(), initialDataFlow.getState().name(), dataFlow.getDataAddress(), null);
                    } else {
                        response = new DataFlowStatusMessage(id, dataFlow.getId(), initialDataFlow.getState().name(), null, null);
                    }

                    return save(dataFlow).map(it -> response);
                });
    }


    public Result<DataFlowStatusMessage> start(String controlplaneId, DataFlowStartMessage message) {
        var initialDataFlow = DataFlow.newInstance()
                .id(message.processId())
                .state(DataFlow.State.INITIATING)
                .dataAddress(message.dataAddress())
                .callbackAddress(message.callbackAddress())
                .transferType(message.transferType())
                .datasetId(message.datasetId())
                .agreementId(message.agreementId())
                .participantId(message.participantId())
                .counterPartyId(message.counterPartyId())
                .dataspaceContext(message.dataspaceContext())
                .controlplaneId(controlplaneId)
                .type(DataFlow.Type.PROVIDER)
                .build();

        return checkControlPlane(controlplaneId)
                .compose(v -> onStart.action(initialDataFlow))
                .compose(dataFlow -> {
                    if (dataFlow.isInitiating()) {
                        dataFlow.transitionToStarted();
                    }

                    DataFlowStatusMessage response;
                    if (dataFlow.isStarted() && dataFlow.isPull()) {
                        response = new DataFlowStatusMessage(id, dataFlow.getId(), dataFlow.getState().name(), dataFlow.getDataAddress(), null);
                    } else {
                        response = new DataFlowStatusMessage(id, dataFlow.getId(), dataFlow.getState().name(), null, null);
                    }
                    return save(dataFlow).map(it -> response);
                });
    }

    public Result<Void> suspend(String flowId, DataFlowSuspendMessage message) {
        return dataFlowStore.findById(flowId)
                .map(dataFlow -> {
                    dataFlow.transitionToSuspended(message.reason());
                    return dataFlow;
                })
                .compose(onSuspend::action)
                .compose(dataFlowStore::save)
                .map(it -> null);
    }

    public Result<DataFlowStatusMessage> resume(String flowId, DataFlowResumeMessage message) {
        return dataFlowStore.findById(flowId)
                .map(dataFlow -> {
                    if (message.dataAddress() != null) {
                        dataFlow.setDataAddress(message.dataAddress());
                    }
                    return dataFlow;
                })
                .compose(onResume::action)
                .compose(dataFlow -> {
                    dataFlow.transitionToStarted();

                    var response = new DataFlowStatusMessage(id, flowId, dataFlow.getState().name(), dataFlow.getDataAddress(), null);

                    return save(dataFlow).map(it -> response);
                });
    }

    public Result<Void> terminate(String dataFlowId, DataFlowTerminateMessage message) {
        return dataFlowStore.findById(dataFlowId)
                .map(dataFlow -> {
                    dataFlow.transitionToTerminated(message.reason());
                    return dataFlow;
                })
                .compose(onTerminate::action)
                .compose(dataFlowStore::save)
                .map(it -> null);
    }

    /**
     * Notify the control plane that the data flow has been prepared.
     *
     * @param dataFlowId the data flow id.
     */
    public Result<Void> notifyPrepared(String dataFlowId, OnPrepare onPrepare) {
        return dataFlowStore.findById(dataFlowId)
                .compose(onPrepare::action)
                .compose(dataFlow -> {
                    dataFlow.transitionToPrepared();
                    var message = new DataFlowStatusMessage(id, dataFlowId, dataFlow.getState().name(), dataFlow.getDataAddress(), null);

                    return notifyControlPlane("prepared", dataFlow, message);

                });
    }

    /**
     * Notify the control plane that the data flow has been started.
     *
     * @param dataFlowId the data flow id.
     */
    public Result<Void> notifyStarted(String dataFlowId, OnStart onStart) {
        return dataFlowStore.findById(dataFlowId)
                .compose(onStart::action)
                .compose(dataFlow -> {
                    dataFlow.transitionToStarted();

                    var message = new DataFlowStatusMessage(id, dataFlowId, dataFlow.getState().name(), dataFlow.getDataAddress(), null);

                    return notifyControlPlane("started", dataFlow, message);

                });
    }

    /**
     * Notify the control plane that the data flow has been completed.
     *
     * @param dataFlowId id of the data flow
     */
    public Result<Void> notifyCompleted(String dataFlowId) {
        return dataFlowStore.findById(dataFlowId)
                .compose(dataFlow -> {
                    dataFlow.transitionToCompleted();

                    return notifyControlPlane("completed", dataFlow, emptyMap());
                });
    }

    /**
     * Notify the control plane that the data flow failed for some reason
     *
     * @param dataFlowId id of the data flow
     * @param throwable  the error
     */
    public Result<Void> notifyErrored(String dataFlowId, Throwable throwable) {
        return dataFlowStore.findById(dataFlowId)
                .compose(dataFlow -> {
                    dataFlow.transitionToTerminated(throwable.getMessage());

                    var message = new DataFlowStatusMessage(id, dataFlowId, dataFlow.getState().name(), null, throwable.getMessage());

                    return notifyControlPlane("errored", dataFlow, message);
                });
    }

    public Result<Void> started(String flowId, DataFlowStartedNotificationMessage startedNotificationMessage) {
        return dataFlowStore.findById(flowId)
                .map(dataFlow -> {
                    dataFlow.setDataAddress(startedNotificationMessage.dataAddress());
                    return dataFlow;
                })
                .compose(onStarted::action)
                .compose(dataFlow -> {
                    dataFlow.transitionToStarted();
                    return save(dataFlow);
                });
    }

    /**
     * Received notification that the flow has been completed
     *
     * @param flowId id of the data flow
     * @return result indicating whether data flow was completed successfully
     */
    public Result<Void> completed(String flowId) {
        return dataFlowStore.findById(flowId).compose(onCompleted::action)
                .compose(dataFlow -> {
                    dataFlow.transitionToCompleted();
                    return save(dataFlow);
                });
    }

    public Result<Void> registerOn(String controlPlaneEndpoint) {

        var message = new DataPlaneRegistrationMessage(id, endpoint, transferTypes, labels);

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

    private Result<Void> notifyControlPlane(String action, DataFlow dataFlow, Object message) {
        return toJson(message)
                .map(body -> {
                    var endpoint = dataFlow.callbackEndpointFor(action);
                    var requestBuilder = HttpRequest.newBuilder()
                            .uri(endpoint)
                            .header("content-type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body));

                    controlPlaneStore.findById(dataFlow.getControlplaneId())
                            .compose(controlPlane -> {
                                var authorizationProfile = controlPlane.authorization();
                                if (authorizationProfile != null) {
                                    var authorization = authorizations.get(authorizationProfile.getType());
                                    return authorization.authorizationHeader(authorizationProfile);
                                }
                                return Result.failure(new ResourceNotFoundException("ControlPlane has no authorization"));
                            })
                            .onSuccess(authorizationHeader -> requestBuilder.header(AUTHORIZATION, authorizationHeader));

                    return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.discarding());
                })
                .compose(response -> {
                    var successful = response.statusCode() >= 200 && response.statusCode() < 300;
                    if (successful) {
                        return save(dataFlow);
                    }

                    return Result.failure(new DataFlowNotifyControlPlaneFailed(action, response));
                });
    }

    private Result<String> toJson(Object message) {
        try {
            return Result.success(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            return Result.failure(e);
        }
    }

    public ControlPlaneStore controlPlaneStore() {
        return controlPlaneStore;
    }

    public Result<Void> registerControlPlane(ControlPlaneRegistrationMessage message) {
        for (var auth : message.authorization()) {
            if (!authorizations.containsKey(auth.getType())) {
                return Result.failure(new AuthorizationNotSupported(auth));
            }
        }

        var controlPlane = ControlPlane.newInstance()
                .id(message.controlplaneId())
                .endpoint(message.endpoint())
                .authorization(message.authorization())
                .build();

        return controlPlaneStore.save(controlPlane);
    }

    public Result<Void> deleteControlPlane(String id) {
        return controlPlaneStore.delete(id);
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

        public Builder endpoint(URI endpoint) {
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

        public Builder onSuspend(OnSuspend onSuspend) {
            dataplane.onSuspend = onSuspend;
            return this;
        }

        public Builder onResume(OnResume onResume) {
            dataplane.onResume = onResume;
            return this;
        }

        public Builder onTerminate(OnTerminate onTerminate) {
            dataplane.onTerminate = onTerminate;
            return this;
        }

        public Builder registerAuthorization(Authorization authorization) {
            dataplane.authorizations.put(authorization.type(), authorization);
            return this;
        }
    }
}
