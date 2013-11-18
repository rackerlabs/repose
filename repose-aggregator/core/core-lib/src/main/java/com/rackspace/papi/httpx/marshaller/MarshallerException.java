package com.rackspace.papi.httpx.marshaller;

/**
 * @author fran
 */
public class MarshallerException extends RuntimeException {
    public MarshallerException(String message, Throwable throwable) {
        super(message, throwable);        
    }
}
