package org.openrepose.services.datastore.impl.distributed;

import org.openrepose.services.datastore.distributed.ClusterConfiguration;
import org.openrepose.services.datastore.hash.MD5MessageDigestFactory;
import org.openrepose.services.datastore.impl.distributed.remote.RemoteCommandExecutor;
import org.openrepose.services.datastore.Datastore;
import org.openrepose.services.datastore.DatastoreManager;

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
