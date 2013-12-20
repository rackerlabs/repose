package com.rackspace.papi.components.datastore.impl.distributed;

public class MalformedCacheRequestException extends RuntimeException {

    public MalformedCacheRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public MalformedCacheRequestException(String message) {
        super(message);
    }
}
