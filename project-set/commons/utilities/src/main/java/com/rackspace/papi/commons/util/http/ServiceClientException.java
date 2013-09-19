package com.rackspace.papi.commons.util.http;

import com.rackspace.papi.service.httpclient.HttpClientNotFoundException;

/**
 Custom exception to handle apache http client exceptions in Serviceclient
**/
public class ServiceClientException extends Throwable {
    public ServiceClientException(String message, HttpClientNotFoundException e) {
        super(message,e.getCause());
    }
}
