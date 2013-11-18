package com.rackspace.papi.service.serviceclient.akka;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;

import java.util.Map;

/**
 * Interface to provide Akka and futures support to Auth client
 */
public interface AkkaServiceClient {
    ServiceClientResponse get(String token, String uri, Map<String, String> headers);
    void shutdown();
}
