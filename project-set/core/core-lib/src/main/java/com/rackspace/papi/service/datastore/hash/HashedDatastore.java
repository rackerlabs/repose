package com.rackspace.papi.service.datastore.hash;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.StoredElement;
import java.util.concurrent.TimeUnit;

public interface HashedDatastore extends Datastore {

    StoredElement getByHash(String encodedHashString) throws DatastoreOperationException;

    void putByHash(String encodedHashString, byte[] value) throws DatastoreOperationException;

    void putByHash(String encodedHashString, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException;
}
