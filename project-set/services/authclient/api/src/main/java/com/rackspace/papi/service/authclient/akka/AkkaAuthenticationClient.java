package com.rackspace.papi.service.authclient.akka;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;

import java.util.Map;

/**
 * Interface to provide Akka and futures support to Auth client
 */
public interface AkkaAuthenticationClient {
    ServiceClientResponse validateToken(String token, String uri, Map<String, String> headers);

    /**
     * Shutdown all open Actors
     */
    void shutdown();
}
