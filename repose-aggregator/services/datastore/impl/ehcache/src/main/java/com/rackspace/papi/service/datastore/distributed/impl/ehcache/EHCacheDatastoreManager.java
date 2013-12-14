package com.rackspace.papi.service.datastore.distributed.impl.ehcache;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.yammer.metrics.ehcache.InstrumentedEhcache;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;

public class EHCacheDatastoreManager implements DatastoreManager {

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

      cacheName = CACHE_NAME_PREFIX + cacheManagerInstance.getName();

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
      if (cacheManagerInstance != null) {
          cacheManagerInstance.removalAll();
          cacheManagerInstance.shutdown();
      }
   }
}
