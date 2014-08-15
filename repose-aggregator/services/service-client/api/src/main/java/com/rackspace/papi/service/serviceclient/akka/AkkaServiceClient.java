package com.rackspace.papi.service.serviceclient.akka;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;

import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * Interface to provide Akka and futures support to Auth client
 */
public interface AkkaServiceClient {

    // Note: ServiceClientResponse generics are typed to Object to improve interoperability with Scala even though
    //       these types are not necessary in this context
    ServiceClientResponse<Object> get(String token, String uri, Map<String, String> headers);
    ServiceClientResponse<Object> post(String requestKey, String uri, Map<String, String> headers, String payload, MediaType contentMediaType); // TODO: Deprecate
    ServiceClientResponse<Object> post(String requestKey, String uri, Map<String, String> headers, String payload, MediaType contentMediaType, MediaType acceptMediaType);

    void shutdown();
}
