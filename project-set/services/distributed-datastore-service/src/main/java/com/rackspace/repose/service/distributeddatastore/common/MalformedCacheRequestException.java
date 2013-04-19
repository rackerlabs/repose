package com.rackspace.repose.service.distributeddatastore.common;

public class MalformedCacheRequestException extends RuntimeException {

    public MalformedCacheRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public MalformedCacheRequestException(String message) {
        super(message);
    }
}
