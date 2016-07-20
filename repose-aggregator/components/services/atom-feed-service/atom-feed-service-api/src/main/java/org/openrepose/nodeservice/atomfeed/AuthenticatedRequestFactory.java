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
package org.openrepose.nodeservice.atomfeed;

/**
 * A factory which will perform any necessary processing to authenticate a request to the Atom service.
 */
public interface AuthenticatedRequestFactory {

    /**
     * Mutates a request to adhere to some authentication scheme compatible with the Atom service.
     *
     * @param feedReadRequest A mutable container for request data that will be sent to the Atom service.
     * @param context         A context object which contains information related to the request.
     * @return The {@link FeedReadRequest} with authentication mutations applied.
     * @throws AuthenticationRequestException if the request fails to authenticate.
     */
    FeedReadRequest authenticateRequest(FeedReadRequest feedReadRequest, AuthenticationRequestContext context) throws AuthenticationRequestException;

    /**
     * This method will be called anytime authentication credentials on a request that has been processed by the
     * authenticateRequest method are found to be invalid.
     */
    void onInvalidCredentials();

    /**
     * Sets the configured Connection Pool Id.
     * <p>
     * NOTE: This default implementation does nothing and will need to be overridden if the value is required.
     */
    default void setConnectionPoolId(String poolId) {
    }
}
