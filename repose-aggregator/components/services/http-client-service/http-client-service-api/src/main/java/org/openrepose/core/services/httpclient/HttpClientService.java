/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.httpclient;

import java.util.Set;

/**
 * HttpClientService - service that manages the lifecycle and configuration of HttpClients
 */
public interface HttpClientService {

    /**
     * See {@link #getClient(String)}.
     *
     * @return the default configured HttpClient
     */
    HttpClientContainer getDefaultClient();

    /**
     * Given an identifier, will return a corresponding HttpClient.
     * <p/>
     * Implementations should return the default client if an unmapped clientId or null is passed.
     * <p/>
     * Users of the HttpClientService should retrieve a client for one-time use, and then release the client
     * using the releaseClient() method. Following this will ensure there are no connection leaks, and also
     * ensures that the user of the HttpClientService always uses the most up-to-date configured
     * HTTPClient. Users of the HttpClientService are strongly recommended to **NOT** reuse an HttpClient
     * retrieved from this method for multiple calls, as there's the possibility that the underlying
     * connection manager may become stale and connections may be timed out after some period of staleness.
     *
     * @param clientId Returns an HttpClient instance that corresponds to the default id, or the default client
     *                 if null is passed for the identifier.
     * @return a HttpClient which corresponds to the clientId parameter
     */
    HttpClientContainer getClient(String clientId);

    /**
     * Used to release a client when the client is no longer in use.  Users of the HttpClientService users should
     * release a client immediately after retrieving it in case the client has been decommissioned.
     *
     * @param httpClientContainer Response received by the user from the getClient() call
     */
    void releaseClient(HttpClientContainer httpClientContainer);

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
}
