package com.rackspace.cloud.valve.http.proxy.httpclient;

import java.io.IOException;
import org.apache.commons.httpclient.HttpMethod;

public interface ProcessableRequest {
    public HttpMethod process(HttpRequestProcessor processor) throws IOException;
}
