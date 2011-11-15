package com.rackspace.auth.openstack.ids;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import java.io.Serializable;

/*
 *  Copyright 2010 Rackspace.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */


import java.io.Serializable;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

/**
 *
 * @author jhopper
 */
public class TokenCache {
    private static final String CACHE_NAME = "Rackspace-Default-API-Auth-Token-Cache";

    private final Cache tokenCache;

    public TokenCache(CacheManager ehCacheManager) {
        if (!ehCacheManager.cacheExists(CACHE_NAME)) {
            tokenCache = new Cache(
                    new CacheConfiguration()
                        .name(CACHE_NAME)
                        .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU)
                        .overflowToDisk(false)
                        .diskPersistent(false)
                        .diskExpiryThreadIntervalSeconds(0)
                        .eternal(false));

            ehCacheManager.addCache(tokenCache);
        } else {
            tokenCache = ehCacheManager.getCache(CACHE_NAME);
        }
    }

    public void removeAll(boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        tokenCache.removeAll(doNotNotifyCacheReplicators);
    }

    public void removeAll() throws IllegalStateException, CacheException {
        tokenCache.removeAll();
    }

    public final boolean remove(Object key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return tokenCache.remove(key, doNotNotifyCacheReplicators);
    }

    public final boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return tokenCache.remove(key, doNotNotifyCacheReplicators);
    }

    public final boolean remove(Object key) throws IllegalStateException {
        return tokenCache.remove(key);
    }

    public final boolean remove(Serializable key) throws IllegalStateException {
        return tokenCache.remove(key);
    }

    public final void put(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        tokenCache.put(element);
    }

    public synchronized void dispose() throws IllegalStateException {
        tokenCache.dispose();
    }

    public final Element get(Object key) throws IllegalStateException, CacheException {
        return tokenCache.get(key);
    }

    public final Element get(Serializable key) throws IllegalStateException, CacheException {
        return tokenCache.get(key);
    }
}

