package com.rackspace.papi.service.httpclient;

import java.util.Set;

/**
 * HttpClientService - service that manages the lifecycle and configuration of HttpClients
 */
public interface HttpClientService<CFG, RSP extends HttpClientResponse> {

    /**
     * Given an identifier, will return a corresponding HttpClient.  Implementations should return a
     * default client if null is passed or will throw an HttpClientNotFoundException.  Implementations
     * should NOT return a default client if a named identifier is provided that is not found.
     * <p/>
     * Users of the HttpClientService should retrieve a client for one-time use, and then release the client
     * using the releaseClient() method.  Following this will ensure there are no connection leaks, and also
     * ensures that the user of the HttpClientService always uses the most up-to-date configured
     * HTTPClient.  Users of the HttpClientService are strongly recommended to **NOT** reuse an HttpClient
     * retrieved from this method for multiple calls, as there's the possibility that the underlying
     * connection manager may become stale and connections may be timed out after some period of staleness.
     *
     * @param clientId Returns an HttpClient instance that corresponds to the default id, or the default client
     *                 if null is passed for the identifier.
     * @return an HttpClient
     * @throws HttpClientNotFoundException if client identified by the provided name is not found.
     */
    HttpClientResponse getClient(String clientId) throws HttpClientNotFoundException;

    /**
     * Used to release a client when the client is no longer in use.  Users of the HttpClientService users should
     * release a client immediately after retrieving it in case the client has been decommissioned.
     * @param httpClientResponse Response received by the user from the getClient() call
     */
    void releaseClient(RSP httpClientResponse);

    /**
     * Configure the available clients that can be used via getClient()
     * <p/>
     * Implementations should support dynamic reconfiguration of existing available clients and
     * ensure that a configure() call while
     *
     * @param config
     */
    void configure(CFG config);

    /**
     * Returns true if the given clientId is available to be retrieved via getClient()
     *
     * @param clientId
     * @return
     */
    boolean isAvailable(String clientId);

    /**
     * Returns a set of available client identifiers
     *
     * @return
     */
    Set<String> getAvailableClients();

    /**
     * Shutdown all open connections
     */
    void shutdown();

    /**
     * Returns the maximum number of connections available for the given clientId
     */
    int getMaxConnections(String clientId);
}
