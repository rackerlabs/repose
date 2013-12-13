package com.rackspace.papi.service.datastore.impl.distributed.hash;

import com.rackspace.papi.service.datastore.DistDatastoreConfiguration;
import com.rackspace.papi.service.datastore.hash.MD5MessageDigestFactory;
import com.rackspace.papi.service.datastore.impl.distributed.hash.remote.RemoteCommandExecutor;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;

public class HashRingDatastoreManager implements DatastoreManager {

    private static final String HOST_KEY = "temp-host-key";
    private final HashRingDatastore datastore;

    public HashRingDatastoreManager(DistDatastoreConfiguration configuration, Datastore localDatastore) {
        datastore = new HashRingDatastore(
                new RemoteCommandExecutor(configuration.getProxyService(), HOST_KEY),
                configuration.getClusterView(),
                "",
                localDatastore,
                MD5MessageDigestFactory.getInstance(),
                configuration.getEncodingProvider());
    }

    @Override
    public void destroy() {
    }

    @Override
    public Datastore getDatastore() {
        return datastore;
    }

    @Override
    public boolean isDistributed() {
        return true;
    }
}
