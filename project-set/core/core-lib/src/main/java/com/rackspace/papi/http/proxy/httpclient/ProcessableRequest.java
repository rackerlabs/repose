package com.rackspace.papi.http.proxy.httpclient;

import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;

public interface ProcessableRequest {
    HttpMethod process(HttpRequestProcessor processor) throws IOException;
}
