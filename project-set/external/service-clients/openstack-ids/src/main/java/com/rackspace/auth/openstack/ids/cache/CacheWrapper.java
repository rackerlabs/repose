package com.rackspace.auth.openstack.ids.cache;

/**
 * @author fran via http://ehcache.org/documentation/recipes/wrapper
 *
 * Modified from original recommended pattern to handle time to live (ttl).
 */
public interface CacheWrapper<K, V> {
    void put(K key, V value, int ttl);

    V get(K key);
}
