package com.rackspace.papi.service.datastore.impl.ehcache;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

public class EHCacheDatastoreManager implements DatastoreManager {

    private final CacheManager cacheManagerInstance;

    public EHCacheDatastoreManager(CacheManager cacheManagerInstance) {
        this.cacheManagerInstance = cacheManagerInstance;
    }

    @Override
    public Datastore getDatastore(String key) {
        Cache cache = cacheManagerInstance.getCache(key);
        
        if (cache == null) {
            cache = new Cache(key, 20000, false, false, 5, 2);
            cacheManagerInstance.addCache(cache);
        }
        
        return new EHCacheDatastore(cache);
    }

    @Override
    public boolean isDistributed() {
        return false;
    }
}
