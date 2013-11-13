package com.rackspace.papi.service.httpclient;

public class HttpClientNotFoundException extends Exception {

    public HttpClientNotFoundException(String message) {
        super(message);
    }

    public HttpClientNotFoundException(String message, Throwable cause){
        super(message, cause);
    }
}
