package org.openrepose.services.serviceclient.akka;

public class AkkServiceClientException extends Exception {
    public AkkServiceClientException() {
        super();
    }

    public AkkServiceClientException(String message) {
        super(message);
    }

    public AkkServiceClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public AkkServiceClientException(Throwable cause) {
        super(cause);
    }
}
