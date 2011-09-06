package com.rackspace.papi.commons.util.http.media;

public class MalformedMediaRangeException extends RuntimeException {

    public MalformedMediaRangeException(String message) {
        super(message);
    }

    public MalformedMediaRangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
