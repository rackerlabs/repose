package com.rackspace.papi.service.datastore;

public class DatastoreOperationCanceledException extends RuntimeException {

    public DatastoreOperationCanceledException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatastoreOperationCanceledException(String message) {
        super(message);
    }
}
