package com.rackspace.papi.service.datastore.impl.distributed.hash;

import com.rackspace.papi.service.datastore.impl.distributed.hash.remote.RemoteCommandExecutor;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.encoding.EncodingProvider;
import com.rackspace.papi.service.datastore.hash.MessageDigestFactory;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;

public class HashRingDatastoreManager implements DatastoreManager {

    public static final String DATASTORE_MANAGER_NAME = "distributed/hash-ring";
    private static final String HOST_KEY = "temp-host-key";
    private final HashRingDatastore datastore;
    private boolean available;

    public HashRingDatastoreManager(RequestProxyService proxyService, String hostKey, EncodingProvider encodingProvider, MessageDigestFactory hashProvider, MutableClusterView clusterView, Datastore localDatastore) {
        datastore = new HashRingDatastore(
                new RemoteCommandExecutor(proxyService, HOST_KEY),
                clusterView,
                hostKey,
                localDatastore,
                hashProvider,
                encodingProvider);
        available = true;
    }

    @Override
    public String getName() {
        return DATASTORE_MANAGER_NAME;
    }

    @Override
    public Datastore getDatastore() {
        return datastore;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void destroy() {
        available = false;
    }

    @Override
    public boolean isDistributed() {
        return true;
    }
}