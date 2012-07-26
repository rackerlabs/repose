package com.rackspace.papi.components.clientauth.common;

import com.rackspace.auth.AuthGroups;
import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.StoredElement;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


public class AuthGroupCache {

    private final Datastore store;
    private final String cachePrefix;

    public AuthGroupCache(Datastore store, String cachePrefix) {
        this.store = store;
        this.cachePrefix = cachePrefix;
    }

    private AuthGroups getElementAsType(StoredElement element) {
        return element == null || element.elementIsNull() ? null : element.elementAs(AuthGroups.class);
    }

    public AuthGroups getUserGroup(String userId) {
        AuthGroups candidate = getElementAsType(store.get(cachePrefix + "." + userId));

        return validateGroup(candidate) ? candidate : null;
    }

    public void storeGroups(String userId, AuthGroups groups, int ttl) throws IOException {
        if (userId == null || groups == null || ttl < 0) {
            // TODO Should we throw an exception here?
            return;
        }

        byte[] data = ObjectSerializer.instance().writeObject(groups);

        store.put(cachePrefix + "." + userId, data, ttl, TimeUnit.MILLISECONDS);
    }

    public boolean validateGroup(AuthGroups cachedValue) {
        return cachedValue != null && cachedValue.getGroups() != null;
    }
}
