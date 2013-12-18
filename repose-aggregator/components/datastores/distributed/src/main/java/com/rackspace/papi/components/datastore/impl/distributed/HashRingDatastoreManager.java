package com.rackspace.papi.components.datastore.impl.distributed;

import com.rackspace.papi.components.datastore.distributed.ClusterConfiguration;
import com.rackspace.papi.components.datastore.hash.MD5MessageDigestFactory;
import com.rackspace.papi.components.datastore.impl.distributed.remote.RemoteCommandExecutor;
import com.rackspace.papi.components.datastore.Datastore;
import com.rackspace.papi.components.datastore.DatastoreManager;

public class HashRingDatastoreManager implements DatastoreManager {

    private static final String HOST_KEY = "temp-host-key";
    private final HashRingDatastore datastore;

    public HashRingDatastoreManager(ClusterConfiguration configuration, Datastore localDatastore) {
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
