package com.rackspace.papi.service.datastore.impl.ehcache;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import java.util.UUID;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

public class EHCacheDatastoreManager implements DatastoreManager {

    private static final String CACHE_NAME_PREFIX = "PAPI_LOCAL";
    private final CacheManager cacheManagerInstance;
    private final String cacheName;
    private boolean available;

    public EHCacheDatastoreManager(CacheManager cacheManagerInstance) {
        this.cacheManagerInstance = cacheManagerInstance;

        cacheName = CACHE_NAME_PREFIX + UUID.randomUUID().toString();

        init();
    }

    private void init() {
        final Cache cache = new Cache(cacheName, 20000, false, false, 5, 2);
        cacheManagerInstance.addCache(cache);

        available = true;
    }

    @Override
    public String getName() {
        return DatastoreService.DEFAULT_LOCAL;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void destroy() {
        available = false;

        cacheManagerInstance.removeCache(cacheName);

    }

    @Override
    public Datastore getDatastore() {
        return new EHCacheDatastore(cacheManagerInstance.getCache(cacheName));
    }

    @Override
    public boolean isDistributed() {
        return false;
    }
}
