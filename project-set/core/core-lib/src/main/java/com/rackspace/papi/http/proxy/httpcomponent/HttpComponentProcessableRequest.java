package com.rackspace.papi.http.proxy.httpcomponent;

import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public interface HttpComponentProcessableRequest {
    HttpRequestBase process(HttpComponentRequestProcessor processor) throws IOException;
}
