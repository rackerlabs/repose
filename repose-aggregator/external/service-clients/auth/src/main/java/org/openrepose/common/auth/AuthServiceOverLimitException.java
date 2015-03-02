package org.openrepose.common.auth;

public class AuthServiceOverLimitException extends AuthServiceException {
    final int statusCode;
    final String retryAfter;

    public AuthServiceOverLimitException(String message, int code, String retry) {
        super(message);
        statusCode = code;
        retryAfter = retry;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getRetryAfter() {
        return retryAfter;
    }
}
