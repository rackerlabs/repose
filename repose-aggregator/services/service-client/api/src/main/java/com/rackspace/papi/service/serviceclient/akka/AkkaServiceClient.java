package com.rackspace.papi.service.serviceclient.akka;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;

import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * Interface to provide Akka and futures support to Auth client
 */
public interface AkkaServiceClient {

    public static final String ADMIN_TOKEN_KEY = "ADMIN_TOKEN";

    ServiceClientResponse get(String token, String uri, Map<String, String> headers);
    ServiceClientResponse post(String requestKey, String uri, Map<String, String> headers, String payload, MediaType mediaType);

    void shutdown();
}
