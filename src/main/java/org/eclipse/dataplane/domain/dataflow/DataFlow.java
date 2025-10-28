package org.eclipse.dataplane.domain.dataflow;

import org.eclipse.dataplane.domain.DataAddress;

import java.util.Objects;

// TODO: could it store the messages?
public class DataFlow {

    private String id;
    private State state;
    private String transferType;
    private DataAddress dataAddress;
    private String callbackAddress;
    private String terminationReason;

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

        public Builder dataAddress(DataAddress dataAddress) {
            dataFlow.dataAddress = dataAddress;
            return this;
        }

        public Builder callbackAddress(String callbackAddress) {
            dataFlow.callbackAddress = callbackAddress;
            return this;
        }
    }

    public enum State {
        INITIATING,
        PREPARING,
        PREPARED,
        STARTING,
        STARTED,
        COMPLETED,
        TERMINATED
    }
}

