package org.openrepose.services.serviceclient.akka;

import org.openrepose.commons.utils.http.ServiceClientResponse;

import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Interface to provide Akka and futures support to Auth client
 */
public interface AkkaServiceClient {

    ServiceClientResponse get(String token, String uri, Map<String, String> headers) throws AkkServiceClientException;
    ServiceClientResponse post(String requestKey, String uri, Map<String, String> headers, String payload, MediaType contentMediaType) throws AkkServiceClientException;

    void shutdown();
}
