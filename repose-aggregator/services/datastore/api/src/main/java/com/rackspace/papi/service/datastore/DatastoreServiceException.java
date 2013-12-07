package com.rackspace.papi.service.datastore;

public class DatastoreServiceException extends RuntimeException {

    public DatastoreServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatastoreServiceException(String message) {
        super(message);
    }
}
