package org.openrepose.services.httpclient;

public class HttpClientNotFoundException extends Exception {

    public HttpClientNotFoundException(String message) {
        super(message);
    }

    public HttpClientNotFoundException(String message, Throwable cause){
        super(message, cause);
    }
}
