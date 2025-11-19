package org.eclipse.dataplane.port.exception;

public class DataplaneNotRegistered extends RuntimeException {
    public DataplaneNotRegistered(String message) {
        super(message);
    }
}
