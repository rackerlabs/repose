package org.openrepose.services.datastore.api;

public class DatastoreOperationException extends RuntimeException {

    public DatastoreOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatastoreOperationException(String message) {
        super(message);
    }
}
