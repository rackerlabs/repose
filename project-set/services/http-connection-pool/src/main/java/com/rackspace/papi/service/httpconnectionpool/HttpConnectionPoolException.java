package com.rackspace.papi.service.httpconnectionpool;

public class HttpConnectionPoolException extends RuntimeException {

    public HttpConnectionPoolException(String message) {
        super(message);
    }

    public HttpConnectionPoolException(String message, Throwable cause){
        super(message, cause);
    }
}
