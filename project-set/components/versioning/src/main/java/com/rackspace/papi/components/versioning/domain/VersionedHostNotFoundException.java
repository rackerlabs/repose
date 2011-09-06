package com.rackspace.papi.components.versioning.domain;

public class VersionedHostNotFoundException extends Exception {

    public VersionedHostNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public VersionedHostNotFoundException(String message) {
        super(message);
    }
}
