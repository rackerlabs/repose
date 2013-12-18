package com.rackspace.papi.components.datastore.impl.replicated;

import com.rackspace.papi.components.datastore.DatastoreOperationException;

import java.util.concurrent.TimeUnit;

public interface NotifiableDatastore {

    boolean remove(String key, boolean notify) throws DatastoreOperationException;
    void put(String key, byte[] value, boolean notify) throws DatastoreOperationException;
    void put(String key, byte[] value, int ttl, TimeUnit timeUnit, boolean notify) throws DatastoreOperationException;
}
