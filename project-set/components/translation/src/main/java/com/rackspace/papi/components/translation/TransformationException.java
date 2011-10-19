package com.rackspace.papi.components.translation;

public class TransformationException extends RuntimeException {
    public TransformationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
