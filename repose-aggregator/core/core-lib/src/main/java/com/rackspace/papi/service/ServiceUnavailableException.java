package com.rackspace.papi.service;

//TODO: Refactor this into a checked exception
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
