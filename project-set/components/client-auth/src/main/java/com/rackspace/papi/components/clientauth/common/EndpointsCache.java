package com.rackspace.papi.components.clientauth.common;

import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.StoredElement;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * This class manages the caching of endpoints.
 */

public class EndpointsCache implements DeleteableCache{

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

    public void storeEndpoints(String token, String endpoints, int ttl) throws IOException {
        if (endpoints == null || token == null || ttl < 0) {
            // TODO Should we throw an exception here?
            return;
        }

        byte[] data = ObjectSerializer.instance().writeObject(endpoints);

        store.put(cachePrefix + "." + token, data, ttl, TimeUnit.MILLISECONDS);
    }

   @Override
   public boolean deleteCacheItem(String userId) {
      return store.remove(userId);
   }
}
