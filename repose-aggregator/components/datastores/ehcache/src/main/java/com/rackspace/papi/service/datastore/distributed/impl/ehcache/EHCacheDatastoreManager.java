package com.rackspace.papi.service.datastore.distributed.impl.ehcache;

import com.rackspace.papi.components.datastore.Datastore;
import com.rackspace.papi.components.datastore.DatastoreManager;
import com.yammer.metrics.ehcache.InstrumentedEhcache;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.slf4j.Logger;

import java.util.UUID;

public class EHCacheDatastoreManager implements DatastoreManager {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(EHCacheDatastoreManager.class);

    private static final String CACHE_NAME_PREFIX = "PAPI_LOCAL";
    private static final String CACHE_MANAGER_NAME = "LocalDatastoreCacheManager";
    private final CacheManager cacheManagerInstance;
    private final String cacheName;
    private Ehcache instrumentedCache;

    public EHCacheDatastoreManager() {

        Configuration defaultConfiguration = new Configuration();
        defaultConfiguration.setName(CACHE_MANAGER_NAME);
        defaultConfiguration.setDefaultCacheConfiguration(new CacheConfiguration().diskPersistent(false));
        defaultConfiguration.setUpdateCheck(false);

        this.cacheManagerInstance = CacheManager.newInstance(defaultConfiguration);

        cacheName = CACHE_NAME_PREFIX + ":" + cacheManagerInstance.getName() + UUID.randomUUID().toString();

        final Ehcache cache = new Cache(cacheName, 20000, false, false, 5, 2);
        cacheManagerInstance.addCache(cache);

        this.instrumentedCache = InstrumentedEhcache.instrument(cache);
    }

    @Override
    public Datastore getDatastore() {
        return new EHCacheDatastore(instrumentedCache);
    }

    @Override
    public boolean isDistributed() {
        return false;
    }

    @Override
    public void destroy() {
        try {
            if (cacheManagerInstance != null) {
                cacheManagerInstance.removalAll();
                cacheManagerInstance.shutdown();
            }
        } catch (Exception e) {
            LOG.warn("Error occurred when shutting down datastore: {}", e.getMessage());
        }
    }
}
