package com.rackspace.papi.service.datastore.impl.ehcache;

import com.rackspace.papi.service.datastore.Datastore;

public class ReposeEHCache implements ReposeEHCacheMBean {

    private static final String AUTH_TOKEN_CACHE_PREFIX = "openstack.identity.token";
    private static final String AUTH_GROUP_CACHE_PREFIX = "openstack.identity.group";
    private final Datastore cache;

    public ReposeEHCache(Datastore cache) {
        this.cache = cache;
    }

    @Override
    public boolean removeTokenAndRoles(String key) {
        return cache.remove(AUTH_TOKEN_CACHE_PREFIX + "." + key);
    }

    @Override
    public boolean removeGroups(String key) {
        return cache.remove(AUTH_GROUP_CACHE_PREFIX + "." + key);
    }
}
