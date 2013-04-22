package com.rackspace.papi.service.datastore.impl.distributed.hash;

import com.rackspace.papi.service.datastore.impl.distributed.common.RemoteBehavior;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.StoredElement;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author zinic
 */
public interface ClusteredDatastore extends Datastore {

   StoredElement get(String key, RemoteBehavior behavior);
   boolean remove(String key, RemoteBehavior behavior);
   void put(String key, byte[] value, RemoteBehavior behavior);
   void put(String key, byte[] value, int ttl, TimeUnit timeUnit, RemoteBehavior behavior);
}
