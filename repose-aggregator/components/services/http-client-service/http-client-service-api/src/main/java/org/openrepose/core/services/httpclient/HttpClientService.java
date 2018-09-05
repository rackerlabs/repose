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

/**
 * Manages the configuration and lifecycle of {@link org.apache.http.client.HttpClient HttpClients}.
 */
public interface HttpClientService {

    /**
     * See {@link #getClient(String)}.
     *
     * @return the default configured {@link org.apache.http.client.HttpClient}
     */
    TrackedHttpClient getDefaultClient();

    /**
     * Given an identifier, will return the corresponding {@link org.apache.http.client.HttpClient}.
     * <p/>
     * Implementations should return the default client if an unmapped clientId or null is passed.
     * <p/>
     * Users of the HttpClientService should retrieve a client for one-time use, and then release the client
     * using the {@link #releaseClient(TrackedHttpClient)} method. This pattern will ensure there are no
     * connection leaks, and also that the user of the HttpClientService always uses the most up-to-date configured
     * {@link org.apache.http.client.HttpClient}. Users of the HttpClientService are strongly recommended
     * not to reuse an {@link org.apache.http.client.HttpClient} retrieved from this method for multiple calls,
     * as there exists the possibility that the underlying connection manager may become stale and connections
     * may be timed out after some period of staleness.
     *
     * @param clientId an identifier for an {@link org.apache.http.client.HttpClient}
     * @return the {@link org.apache.http.client.HttpClient} identified by the clientId parameter
     */
    TrackedHttpClient getClient(String clientId);

    /**
     * Used to release a client when the client is no longer in use.  Users of the HttpClientService users should
     * release a client immediately after retrieving it in case the client has been decommissioned.
     *
     * @param httpClientContainer Response received by the user from the getClient() call
     */
    void releaseClient(TrackedHttpClient httpClientContainer);
}
