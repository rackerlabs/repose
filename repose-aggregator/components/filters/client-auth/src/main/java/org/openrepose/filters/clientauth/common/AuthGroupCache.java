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

import org.openrepose.common.auth.AuthGroups;
import org.openrepose.core.services.datastore.Datastore;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Deprecated
public class AuthGroupCache implements DeleteableCache {

    private final Datastore store;
    private final String cachePrefix;

    public AuthGroupCache(Datastore store, String cachePrefix) {
        this.store = store;
        this.cachePrefix = cachePrefix;
    }

    public AuthGroups getUserGroup(String tenantId) {
        AuthGroups candidate = (AuthGroups) store.get(cacheKey(tenantId));

        return validateGroup(candidate) ? candidate : null;
    }

    private String cacheKey(String id) {
        return cachePrefix + "." + id;
    }

    public void storeGroups(String tenantId, AuthGroups groups, int ttl) throws IOException {
        if (tenantId == null || groups == null || ttl < 0) {
            // TODO Should we throw an exception here?
            return;
        }

        store.put(cacheKey(tenantId), groups, ttl, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean deleteCacheItem(String tenantId) {
        return store.remove(cacheKey(tenantId));
    }

    public boolean validateGroup(AuthGroups cachedValue) {
        return cachedValue != null && cachedValue.getGroups() != null;
    }
}
