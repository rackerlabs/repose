package com.rackspace.papi.commons.util.jmx;

public class JmxException extends RuntimeException {

    public JmxException() {
        super();
    }

    public JmxException(String message) {
        super(message);
    }

    public JmxException(String message, Throwable cause) {
        super(message, cause);
    }
}
