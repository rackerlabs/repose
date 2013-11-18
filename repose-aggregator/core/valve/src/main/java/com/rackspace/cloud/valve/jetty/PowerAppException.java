package com.rackspace.cloud.valve.jetty;

/**
 *
 * @author Dan Daley
 */
public class PowerAppException extends RuntimeException {
    public PowerAppException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

    public PowerAppException(String message) {
        super(message);
    }
  
}
