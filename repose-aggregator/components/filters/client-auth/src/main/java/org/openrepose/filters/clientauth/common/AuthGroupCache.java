package org.openrepose.filters.clientauth.common;

import com.rackspace.auth.AuthGroups;
import com.rackspace.papi.components.datastore.Datastore;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


public class AuthGroupCache implements DeleteableCache{

    private final Datastore store;
    private final String cachePrefix;

    public AuthGroupCache(Datastore store, String cachePrefix) {
        this.store = store;
        this.cachePrefix = cachePrefix;
    }

    public AuthGroups getUserGroup(String tenantId) {
        AuthGroups candidate = (AuthGroups)store.get(cachePrefix + "." + tenantId);

        return validateGroup(candidate) ? candidate : null;
    }

    public void storeGroups(String tenantId, AuthGroups groups, int ttl) throws IOException {
        if (tenantId == null || groups == null || ttl < 0) {
            // TODO Should we throw an exception here?
            return;
        }

        store.put(cachePrefix + "." + tenantId, groups, ttl, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public boolean deleteCacheItem(String tenantId){
       return store.remove(cachePrefix + tenantId);
    }

    public boolean validateGroup(AuthGroups cachedValue) {
        return cachedValue != null && cachedValue.getGroups() != null;
    }
}
