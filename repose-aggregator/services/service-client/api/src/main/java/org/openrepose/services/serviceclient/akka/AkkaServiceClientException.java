package org.openrepose.services.serviceclient.akka;

public class AkkaServiceClientException extends Exception {
    public AkkaServiceClientException() {
        super();
    }

    public AkkaServiceClientException(String message) {
        super(message);
    }

    public AkkaServiceClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public AkkaServiceClientException(Throwable cause) {
        super(cause);
    }
}
