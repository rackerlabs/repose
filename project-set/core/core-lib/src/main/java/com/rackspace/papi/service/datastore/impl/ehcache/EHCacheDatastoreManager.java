package com.rackspace.papi.service.datastore.impl.ehcache;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.yammer.metrics.ehcache.InstrumentedEhcache;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;

public class EHCacheDatastoreManager implements DatastoreManager {

   private static final String CACHE_NAME_PREFIX = "PAPI_LOCAL";
   private final CacheManager cacheManagerInstance;
   private final String cacheName;
   private boolean available;
   private Ehcache instrumentedCache;

   public EHCacheDatastoreManager(CacheManager cacheManagerInstance) {
      this.cacheManagerInstance = cacheManagerInstance;

      cacheName = CACHE_NAME_PREFIX + cacheManagerInstance.getName();

      init();
   }

   private void init() {
      final Ehcache cache = new Cache(cacheName, 20000, false, false, 5, 2);
      cacheManagerInstance.addCache(cache);

      this.instrumentedCache = InstrumentedEhcache.instrument(cache);
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
      return new EHCacheDatastore(instrumentedCache);
   }

   @Override
   public boolean isDistributed() {
      return false;
   }
}
