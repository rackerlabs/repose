package com.rackspace.papi.components.clientauth.common;

import com.rackspace.auth.AuthGroups;
import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.StoredElement;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


public class AuthGroupCache implements DeleteableCache{

    private final Datastore store;
    private final String cachePrefix;

    public AuthGroupCache(Datastore store, String cachePrefix) {
        this.store = store;
        this.cachePrefix = cachePrefix;
    }

    private AuthGroups getElementAsType(StoredElement element) {
        return element == null || element.elementIsNull() ? null : element.elementAs(AuthGroups.class);
    }

    public AuthGroups getUserGroup(String tenantId) {
        AuthGroups candidate = getElementAsType(store.get(cachePrefix + "." + tenantId));

        return validateGroup(candidate) ? candidate : null;
    }

    public void storeGroups(String tenantId, AuthGroups groups, int ttl) throws IOException {
        if (tenantId == null || groups == null || ttl < 0) {
            // TODO Should we throw an exception here?
            return;
        }

        byte[] data = ObjectSerializer.instance().writeObject(groups);

        store.put(cachePrefix + "." + tenantId, data, ttl, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public boolean deleteCacheItem(String tenantId){
       return store.remove(cachePrefix + tenantId);
    }

    public boolean validateGroup(AuthGroups cachedValue) {
        return cachedValue != null && cachedValue.getGroups() != null;
    }
}
