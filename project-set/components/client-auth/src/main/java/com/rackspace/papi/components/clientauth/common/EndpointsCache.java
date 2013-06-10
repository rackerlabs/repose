package com.rackspace.papi.components.clientauth.common;

import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.StoredElement;
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

    private String getElementAsType(StoredElement element) {
        return element == null || element.elementIsNull() ? null : element.elementAs(String.class);
    }

    public String getEndpoints(String token) {
        String candidate = getElementAsType(store.get(cachePrefix + "." + token));

        return candidate;
    }

    public void storeEndpoints(String tokenId, String endpoints, int ttl) throws IOException {
        if (endpoints == null || tokenId == null || ttl < 0) {
            LOG.warn("Null values passed into cache when attempting to store endpoints.");
            return;
        }

        byte[] data = ObjectSerializer.instance().writeObject(endpoints);

        store.put(cachePrefix + "." + tokenId, data, ttl, TimeUnit.MILLISECONDS);
    }

   @Override
   public boolean deleteCacheItem(String userId) {
      return store.remove(userId);
   }
}
