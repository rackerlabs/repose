package com.rackspace.papi.service.datastore;

import java.util.concurrent.TimeUnit;

public interface HashedDatastore extends Datastore {

    StoredElement getByHash(String base64EncodedHashString) throws DatastoreOperationException;
    
    void putByHash(String base64EncodedHashString, byte[] value) throws DatastoreOperationException;
    
    void putByHash(String base64EncodedHashString, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException;
}
