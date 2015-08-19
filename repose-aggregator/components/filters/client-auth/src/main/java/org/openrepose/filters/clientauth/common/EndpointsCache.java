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
package org.openrepose.filters.clientauth.common;

import org.openrepose.core.services.datastore.Datastore;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * This class manages the caching of endpoints.
 */
@Deprecated
public class EndpointsCache implements DeleteableCache {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AuthenticationHandler.class);

    private final Datastore store;
    private final String cachePrefix;

    public EndpointsCache(Datastore store, String cachePrefix) {
        this.store = store;
        this.cachePrefix = cachePrefix;
    }

    public String getEndpoints(String token) {
        String candidate = (String) store.get(cachePrefix + "." + token);

        return candidate;
    }

    public void storeEndpoints(String tokenId, String endpoints, int ttl) throws IOException {
        if (endpoints == null || tokenId == null || ttl < 0) {
            LOG.warn("Null values passed into cache when attempting to store endpoints.");
            return;
        }

        store.put(cachePrefix + "." + tokenId, endpoints, ttl, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean deleteCacheItem(String userId) {
        return store.remove(userId);
    }
}
