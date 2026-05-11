package org.eclipse.dataplane.port.exception;

/**
 * Indicates an error during database interactions, i.e. an error occurred persisting, reading or
 * deleting an entry.
 */
public class PersistenceException extends RuntimeException {

    public PersistenceException(String message) {
        super(message);
    }

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
