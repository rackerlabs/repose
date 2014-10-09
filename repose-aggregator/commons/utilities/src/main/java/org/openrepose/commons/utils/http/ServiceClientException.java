package org.openrepose.commons.utils.http;

import org.openrepose.services.httpclient.HttpClientNotFoundException;

/**
 Custom exception to handle apache http client exceptions in Serviceclient
**/
public class ServiceClientException extends Throwable {
    public ServiceClientException(String message, HttpClientNotFoundException e) {
        super(message,e.getCause());
    }
}
