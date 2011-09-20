package com.rackspace.papi.components.datastore.hash.client;

import com.rackspace.papi.components.datastore.hash.AbstractHashRingDatastore;
import com.rackspace.papi.components.datastore.hash.HashRingDatastore;
import com.rackspace.papi.components.datastore.hash.RemoteConnectionException;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.encoding.EncodingProvider;
import com.rackspace.papi.service.datastore.hash.HashProvider;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HashRingDatastoreClient extends AbstractHashRingDatastore {

    private static final Logger LOG = LoggerFactory.getLogger(HashRingDatastore.class);

    public HashRingDatastoreClient(MutableClusterView clusterView, String datasetPrefix, Datastore localDatastore, HashProvider hashProvider, EncodingProvider encodingProvider) {
        super(clusterView, datasetPrefix, localDatastore, hashProvider, encodingProvider);
    }

    @Override
    protected void clusterMemberDamaged(InetSocketAddress member, MutableClusterView clusterView, RemoteConnectionException ex) {
        LOG.warn(clusterView.localMember().toString()
                + ":: Encountered damaged cluster member: "
                + member.getAddress().toString()
                + ":" + member.getPort()
                + " - Reason: " + ex.getCause().getClass().getName() + ": " + ex.getCause().getMessage());
    }
}