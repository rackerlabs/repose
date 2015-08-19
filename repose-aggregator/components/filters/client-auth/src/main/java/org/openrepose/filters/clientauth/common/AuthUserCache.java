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

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Cache for Auth user.
 */
@Deprecated
public class AuthUserCache implements DeleteableCache {

    private final Datastore store;
    private final String cachePrefix;

    public AuthUserCache(Datastore store, String cachePrefix) {
        this.store = store;
        this.cachePrefix = cachePrefix;
    }

    private String cacheKey(String userId){
        return cachePrefix + "." + userId;
    }

    public void storeUserTokenList(String userId, Set<String> tokens, int ttl) throws IOException {
        if (userId == null || tokens == null || ttl < 0) {
            // TODO Should we throw an exception here?
            return;
        }
        store.put(cacheKey(userId), (Serializable) tokens, ttl, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean deleteCacheItem(String userId) {
        return store.remove(cacheKey(userId));
    }

    public Set<String> getUserTokenList(String userId) {
        Set<String> candidate = (Set<String>) store.get(cacheKey(userId));
        return candidate;
    }
}
