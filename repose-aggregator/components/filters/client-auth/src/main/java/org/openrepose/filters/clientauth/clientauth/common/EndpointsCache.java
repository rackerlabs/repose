package org.openrepose.filters.clientauth.clientauth.common;

import com.rackspace.papi.components.datastore.Datastore;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * This class manages the caching of endpoints.
 */

public class EndpointsCache implements DeleteableCache{

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AuthenticationHandler.class);

    private final Datastore store;
    private final String cachePrefix;

    public EndpointsCache(Datastore store, String cachePrefix) {
        this.store = store;
        this.cachePrefix = cachePrefix;
    }

    public String getEndpoints(String token) {
        String candidate = (String)store.get(cachePrefix + "." + token);

        return candidate;
    }

    public void storeEndpoints(String tokenId, String endpoints, int ttl) throws IOException {
        if (endpoints == null || tokenId == null || ttl < 0) {
            LOG.warn("Null values passed into cache when attempting to store endpoints.");
            return;
        }

        store.put(cachePrefix + "." + tokenId, endpoints, ttl, TimeUnit.MILLISECONDS);
    }

   @Override
   public boolean deleteCacheItem(String userId) {
      return store.remove(userId);
   }
}
