package com.rackspace.papi.service.config;

public class ConfigurationServiceException extends RuntimeException {

    public ConfigurationServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationServiceException(String message) {
        super(message);
    }
}
