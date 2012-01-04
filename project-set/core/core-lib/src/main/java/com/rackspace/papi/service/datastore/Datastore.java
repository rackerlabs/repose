package com.rackspace.papi.service.datastore;

import java.util.concurrent.TimeUnit;

public interface Datastore {

    StoredElement get(String key) throws DatastoreOperationException;
    
    boolean remove(String key) throws DatastoreOperationException;
    
    void put(String key, byte[] value) throws DatastoreOperationException;

    void put(String key, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException;
}
