package com.rackspace.auth.openstack.ids.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

/**
 * @author fran via http://ehcache.org/documentation/recipes/wrapper
 *
 * A simple cache wrapper to hide the use of the Element class.
 * Modified from original recommended pattern to handle time to live (ttl)
 * and to check isExpired on get and merged in non-default configuration of cache.
 */
public class EhcacheWrapper<K, V> implements CacheWrapper<K, V> {

    private static final String CACHE_NAME = "Rackspace-Default-API-Auth-Token-Cache";
    private final Cache tokenCache;

    public EhcacheWrapper(final CacheManager cacheManager)
    {
        if (!cacheManager.cacheExists(CACHE_NAME)) {
            tokenCache = new Cache(
                    new CacheConfiguration()
                        .name(CACHE_NAME)
                        .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU)
                        .overflowToDisk(false)
                        .diskPersistent(false)
                        .diskExpiryThreadIntervalSeconds(0)
                        .eternal(false));

            cacheManager.addCache(tokenCache);
        } else {
            tokenCache = cacheManager.getCache(CACHE_NAME);
        }
    }

    @Override
    public void put(K key, V value, int ttl) {
        Element element = new Element(key, value);
        element.setTimeToLive(ttl);
        getCache().put(element);        
    }

    @Override
    public V get(K key) {
        Element element = getCache().get(key);

        if (element != null && !element.isExpired()) {
            return (V) element.getValue();
        }

        return null;
    }

    public Ehcache getCache()
    {
        return tokenCache;
    }
}
