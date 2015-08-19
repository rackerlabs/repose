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

import org.openrepose.common.auth.AuthToken;
import org.openrepose.core.services.datastore.Datastore;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Deprecated
public class AuthTokenCache implements DeleteableCache {

    private final Datastore store;
    private final String cachePrefix;

    public AuthTokenCache(Datastore store, String cachePrefix) {
        this.store = store;
        this.cachePrefix = cachePrefix;
    }

    public AuthToken getUserToken(String tokenId) {
        AuthToken candidate = (AuthToken) store.get(cachePrefix + "." + tokenId); //Looking into the datastore for this token.
        return validateToken(candidate) ? candidate : null;
    }

    public void storeToken(String tokenId, AuthToken token, int ttl) throws IOException {
        if (tokenId == null || token == null || ttl < 0) {
            // TODO Should we throw an exception here?
            return;
        }

        store.put(cachePrefix + "." + tokenId, token, ttl, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean deleteCacheItem(String userId) {
        return store.remove(cachePrefix + "." + userId);
    }

    public boolean validateToken(AuthToken cachedValue) {
        return cachedValue != null && cachedValue.getTokenId() != null && cachedValue.safeTokenTtl() > 0;
    }
}
