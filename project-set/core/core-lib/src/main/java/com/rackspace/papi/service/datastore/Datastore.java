package com.rackspace.papi.service.datastore;

import java.util.concurrent.TimeUnit;

public interface Datastore {

    StoredElement get(String key) throws DatastoreOperationException;

    boolean remove(String key) throws DatastoreOperationException;

    boolean remove(String key, boolean notify) throws DatastoreOperationException;

    void put(String key, byte[] value) throws DatastoreOperationException;

    void put(String key, byte[] value, boolean notify) throws DatastoreOperationException;

    void put(String key, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException;

    void put(String key, byte[] value, int ttl, TimeUnit timeUnit, boolean notify) throws DatastoreOperationException;

    void removeAllCacheData();
}
