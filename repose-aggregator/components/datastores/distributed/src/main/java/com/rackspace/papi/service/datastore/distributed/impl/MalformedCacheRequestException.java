package com.rackspace.papi.service.datastore.distributed.impl;

public class MalformedCacheRequestException extends RuntimeException {

    public MalformedCacheRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public MalformedCacheRequestException(String message) {
        super(message);
    }
}
