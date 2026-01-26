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
 *
 */

package org.eclipse.dataplane.domain.dataflow;

import org.eclipse.dataplane.domain.DataAddress;

import java.util.List;
import java.util.Map;
import java.util.Objects;

// TODO: could it store the messages?
public class DataFlow {

    private String id;
    private State state;
    private String transferType;
    private String datasetId;
    private String agreementId;
    private String participantId;
    private String counterPartyId;
    private String dataspaceContext;
    private String callbackAddress;
    private String suspensionReason;
    private String terminationReason;
    private List<String> labels;
    private Map<String, Object> metadata;
    private DataAddress dataAddress;

    public static DataFlow.Builder newInstance() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public State getState() {
        return state;
    }

    public DataAddress getDataAddress() {
        return dataAddress;
    }

    public String getCallbackAddress() {
        return callbackAddress;
    }

    public String getTransferType() {
        return transferType;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public String getParticipantId() {
        return participantId;
    }

    public String getCounterPartyId() {
        return counterPartyId;
    }

    public String getDataspaceContext() {
        return dataspaceContext;
    }

    public String getSuspensionReason() {
        return suspensionReason;
    }

    public String getTerminationReason() {
        return terminationReason;
    }

    public List<String> getLabels() {
        return labels;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void transitionToPrepared() {
        state = State.PREPARED;
    }

    public void transitionToPreparing() {
        state = State.PREPARING;
    }

    public void transitionToStarting() {
        state = State.STARTING;
    }

    public void transitionToStarted() {
        state = State.STARTED;
    }

    public void transitionToSuspended(String reason) {
        state = State.SUSPENDED;
        this.suspensionReason = reason;
    }

    public void transitionToCompleted() {
        state = State.COMPLETED;
    }

    public void transitionToTerminated(String reason) {
        state = State.TERMINATED;
        terminationReason = reason;
    }

    public boolean isPush() {
        return transferType.split("-")[1].equalsIgnoreCase("PUSH");
    }

    public boolean isInitiating() {
        return state == State.INITIATING;
    }

    public boolean isPrepared() {
        return state == State.PREPARED;
    }

    public boolean isStarted() {
        return state == State.STARTED;
    }

    public boolean isPull() {
        return transferType.split("-")[1].equalsIgnoreCase("PULL");
    }

    public void setDataAddress(DataAddress dataAddress) {
        this.dataAddress = dataAddress;
    }

    public static class Builder {
        private final DataFlow dataFlow = new DataFlow();

        private Builder() {

        }

        public DataFlow build() {
            Objects.requireNonNull(dataFlow.id);

            if (dataFlow.state == null) {
                dataFlow.state = State.INITIATING;
            }

            return dataFlow;
        }

        public Builder id(String id) {
            dataFlow.id = id;
            return this;
        }

        public Builder state(State state) {
            dataFlow.state = state;
            return this;
        }

        public Builder transferType(String transferType) {
            dataFlow.transferType = transferType;
            return this;
        }

        public Builder datasetId(String datasetId) {
            dataFlow.datasetId = datasetId;
            return this;
        }

        public Builder agreementId(String agreementId) {
            dataFlow.agreementId = agreementId;
            return this;
        }

        public Builder participantId(String participantId) {
            dataFlow.participantId = participantId;
            return this;
        }

        public Builder counterPartyId(String counterPartyId) {
            dataFlow.counterPartyId = counterPartyId;
            return this;
        }

        public Builder dataspaceContext(String dataspaceContext) {
            dataFlow.dataspaceContext = dataspaceContext;
            return this;
        }

        public Builder labels(List<String> labels) {
            dataFlow.labels = labels;
            return this;
        }

        public Builder dataAddress(DataAddress dataAddress) {
            dataFlow.dataAddress = dataAddress;
            return this;
        }

        public Builder callbackAddress(String callbackAddress) {
            dataFlow.callbackAddress = callbackAddress;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            dataFlow.metadata = metadata;
            return this;
        }
    }

    public enum State {
        INITIATING,
        PREPARING,
        PREPARED,
        STARTING,
        STARTED,
        SUSPENDED,
        COMPLETED,
        TERMINATED
    }
}

