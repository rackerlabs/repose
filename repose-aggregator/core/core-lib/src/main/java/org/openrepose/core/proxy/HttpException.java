package org.openrepose.core.proxy;

public class HttpException extends Exception {
    public HttpException(String message) {
        super(message);
    }
    
    public HttpException(String message, Throwable cause) {
        super(message, cause);
    }
}
