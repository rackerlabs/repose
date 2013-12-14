package com.rackspace.papi.service.datastore.distributed;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.StoredElement;

import java.util.concurrent.TimeUnit;

public interface DistributedDatastore extends Datastore {

    public void put(String name, byte[] id, final byte[] value, final int ttl, final TimeUnit timeUnit,
                    RemoteBehavior initialBehavior);

    public boolean remove(String name, byte[] id, RemoteBehavior initialBehavior);

    public StoredElement get(String name, byte[] id, RemoteBehavior initialBehavior);

}
