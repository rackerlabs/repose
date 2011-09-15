package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.impl.AbstractMapDatastoreManager;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HashRingDatastoreManager extends AbstractMapDatastoreManager<HashRingDatastore> {

    private static final Logger LOG = LoggerFactory.getLogger(HashRingDatastoreManager.class);
    public static final String DATASTORE_MANAGER_NAME = "distributed/hash-ring";
    private final Datastore localDatastore;
    private final MutableClusterView clusterView;
    private final RemoteCacheClient remoteHttpCacheClient;

    public HashRingDatastoreManager(String hostKey, MutableClusterView clusterView, Datastore localDatastore) {
        super(new HashMap<String, HashRingDatastore>());

        this.localDatastore = localDatastore;
        this.clusterView = clusterView;

        final RemoteHttpCacheClientImpl newClient = new RemoteHttpCacheClientImpl();
        newClient.setHostKey(hostKey);

        remoteHttpCacheClient = newClient;
    }

    @Override
    protected HashRingDatastore newDatastore(String key) {
        try {
            final HashRingDatastore datastore = new MD5HashRingDatastore(key, clusterView, localDatastore);
            datastore.setRemoteCacheClient(remoteHttpCacheClient);

            return datastore;
        } catch (NoSuchAlgorithmException algorithmException) {
            LOG.error("Unable to create hash-ring datastore. Hashing algorithm is missing. Reason: " + algorithmException.getMessage(), algorithmException);

            throw new DatastoreOperationException("Unable to create hash-ring datastore. Hashing algorithm is missing. Reason: " + algorithmException.getMessage(), algorithmException);
        }
    }

    @Override
    public boolean isDistributed() {
        return true;
    }
}