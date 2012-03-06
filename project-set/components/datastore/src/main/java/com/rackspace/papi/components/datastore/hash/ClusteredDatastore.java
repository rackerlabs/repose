package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.StoredElement;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author zinic
 */
public interface ClusteredDatastore extends Datastore {

   StoredElement get(String key, RemoteBehavior behavior) throws DatastoreOperationException;

   boolean remove(String key, RemoteBehavior behavior) throws DatastoreOperationException;

   void put(String key, byte[] value, RemoteBehavior behavior) throws DatastoreOperationException;

   void put(String key, byte[] value, int ttl, TimeUnit timeUnit, RemoteBehavior behavior) throws DatastoreOperationException;
}
