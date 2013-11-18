package com.rackspace.papi.components.datastore.common;

public class MalformedCacheRequestException extends RuntimeException {

    public MalformedCacheRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public MalformedCacheRequestException(String message) {
        super(message);
    }
}
