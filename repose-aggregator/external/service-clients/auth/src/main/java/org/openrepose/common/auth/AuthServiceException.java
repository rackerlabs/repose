package org.openrepose.common.auth;

public class AuthServiceException extends Exception {
    public AuthServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthServiceException(String message) {
        super(message);
    }
}
