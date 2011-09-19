package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.encoding.EncodingProvider;
import com.rackspace.papi.service.datastore.hash.HashProvider;
import com.rackspace.papi.service.datastore.impl.AbstractHashedDatastore;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHashRingDatastore extends AbstractHashedDatastore {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractHashRingDatastore.class);
    private final MutableClusterView clusterView;
    private final Datastore localDatastore;
    private RemoteCacheClient remoteCache;

    public AbstractHashRingDatastore(MutableClusterView clusterView, String datastorePrefix, Datastore localDatastore, HashProvider hashProvider, EncodingProvider encodingProvider) {
        super(datastorePrefix, encodingProvider, hashProvider);

        this.clusterView = clusterView;
        this.localDatastore = localDatastore;
    }

    protected abstract void clusterMemberDamaged(InetSocketAddress member, MutableClusterView clusterView, RemoteConnectionException ex);

    public void setRemoteCacheClient(RemoteCacheClient remoteCache) {
        this.remoteCache = remoteCache;
    }

    private InetSocketAddress getTarget(byte[] hashBytes) {
        final InetSocketAddress[] ringMembers = clusterView.members();

        if (ringMembers.length <= 0) {
            LOG.warn("Wraning! No members to route to in datastore cluster. Returning local node.");
            
            return clusterView.localMember();
        }

        final BigInteger ringSliceSize = getHashProvider().maxValue().divide(BigInteger.valueOf(ringMembers.length));
        final int memberAddress = new BigInteger(hashBytes).divide(ringSliceSize).abs().intValue();

        if (memberAddress > ringMembers.length) {
            throw new UnaddressableKeyException("Unable to address given key");
        }

//        final StringBuilder builder = new StringBuilder("[");
//        
//        for (InetSocketAddress addr : ringMembers) {
//            builder.append(addr.toString()).append(":").append(addr.getPort()).append(",");
//        }
        
//        builder.deleteCharAt(builder.length() - 1).append("] - Selected: ").append(memberAddress).append(" - Local: ").append(clusterView.localMember().toString()).append(":").append(clusterView.localMember().getPort());
        
//        LOG.info(builder.toString());
        
        return ringMembers[memberAddress];
    }

    @Override
    protected StoredElement get(String name, byte[] id) throws DatastoreOperationException {
        InetSocketAddress target;

        while (!(target = getTarget(id)).equals(clusterView.localMember())) {
            LOG.debug(clusterView.localMember().toString() + ":: Routing datastore get request for, \"" + name + "\" to: " + target.toString());

            try {
                return remoteCache.get(name, target);
            } catch (RemoteConnectionException rce) {
                clusterMemberDamaged(target, clusterView, rce);
            }
        }

        return localDatastore.get(name);
    }

    @Override
    protected void put(String name, byte[] id, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
        InetSocketAddress target;

        while (!(target = getTarget(id)).equals(clusterView.localMember())) {
            LOG.debug(clusterView.localMember().toString() + ":: Routing datastore get request for, \"" + name + "\" to: " + target.toString());

            try {
                remoteCache.put(name, value, ttl, timeUnit, target);
                return;
            } catch (RemoteConnectionException rce) {
                clusterMemberDamaged(target, clusterView, rce);
            }
        }
        
        localDatastore.put(name, value, ttl, timeUnit);
    }
}