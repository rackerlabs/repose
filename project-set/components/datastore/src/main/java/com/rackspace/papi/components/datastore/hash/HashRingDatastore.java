package com.rackspace.papi.components.datastore.hash;

import com.rackspace.papi.service.datastore.impl.CoalescentDatastoreWrapper;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.HashedDatastore;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HashRingDatastore extends CoalescentDatastoreWrapper implements HashedDatastore {

    private static final Logger LOG = LoggerFactory.getLogger(HashRingDatastore.class);
    private final MutableClusterView clusterView;
    private final String datastorePrefix;
    private RemoteCacheClient remoteCache;

    public HashRingDatastore(String datastorePrefix, MutableClusterView clusterView, Datastore localDatastore) {
        super(localDatastore);

        this.datastorePrefix = datastorePrefix;
        this.clusterView = clusterView;
    }

    public void setRemoteCacheClient(RemoteCacheClient remoteCache) {
        this.remoteCache = remoteCache;
    }

    protected abstract BigInteger maxValue();

    protected abstract byte[] hash(String key);

    protected abstract String hashBytesToSTring(byte[] hash);

    protected abstract byte[] stringToHashBytes(String hash);

    private byte[] getHash(String key) {
        return hash(datastorePrefix + key);
    }

    public InetSocketAddress getTarget(byte[] hashBytes) {
        final InetSocketAddress[] ringMembers = clusterView.members();

        if (ringMembers.length <= 0) {
            return clusterView.localMember();
        }

        final BigInteger ringSliceSize = maxValue().divide(BigInteger.valueOf(ringMembers.length));
        final int memberAddress = new BigInteger(hashBytes).divide(ringSliceSize).abs().intValue();

        if (memberAddress > ringMembers.length) {
            throw new UnaddressableKeyException("Unable to address given key");
        }

        return ringMembers[memberAddress];
    }

    @Override
    public StoredElement get(String key) throws DatastoreOperationException {
        final byte[] keyHash = getHash(key);

        return get(hashBytesToSTring(keyHash), keyHash);
    }

    @Override
    public StoredElement getByHash(String encodedHashString) throws DatastoreOperationException {
        return get(encodedHashString, stringToHashBytes(encodedHashString));
    }

    public StoredElement get(String name, byte[] id) throws DatastoreOperationException {
        InetSocketAddress target = getTarget(id);

        while (!target.equals(clusterView.localMember())) {
            LOG.debug(clusterView.localMember().toString() + ":: Routing datastore get request for, \"" + name + "\" to: " + target.toString());

            try {
                return remoteCache.get(name, target);
            } catch (IOException ex) {
                LOG.info(clusterView.localMember().toString() + ":: Dropping member: " + target.getAddress().toString() + ":" + target.getPort() + " - Reason: " + ex.getClass().getName() + ": " + ex.getMessage());
                
                clusterView.memberDropoped(target);
                target = getTarget(id);

//                throw new DatastoreOperationException("Unable to communicate with member " + target.getAddress().toString() + ":" + target.getPort(), ex);
            }
        }

        return super.get(name);
    }

    @Override
    public void put(String key, byte[] value) throws DatastoreOperationException {
        put(key, value, 3, TimeUnit.MINUTES);
    }

    @Override
    public void put(String key, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
        final byte[] keyHash = getHash(key);

        put(hashBytesToSTring(keyHash), keyHash, value, ttl, timeUnit);
    }

    @Override
    public void putByHash(String encodedHashString, byte[] value) {
        put(encodedHashString, stringToHashBytes(encodedHashString), value, 3, TimeUnit.MINUTES);
    }

    @Override
    public void putByHash(String encodedHashString, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
        put(encodedHashString, stringToHashBytes(encodedHashString), value, ttl, timeUnit);
    }

    public void put(String name, byte[] id, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
        InetSocketAddress target = getTarget(id);

        while (!target.equals(clusterView.localMember())) {
            LOG.debug(clusterView.localMember().toString() + ":: Routing datastore get request for, \"" + name + "\" to: " + target.toString());

            try {
                remoteCache.put(name, value, ttl, timeUnit, target);
                return;
            } catch (IOException ex) {
                LOG.info(clusterView.localMember().toString() + ":: Dropping member: " + target.getAddress().toString() + ":" + target.getPort() + " - Reason: " + ex.getClass().getName() + ": " + ex.getMessage());
                
                clusterView.memberDropoped(target);
                target = getTarget(id);

//                throw new DatastoreOperationException("Unable to communicate with member " + target.getAddress().toString() + ":" + target.getPort(), ex);
            }
        }

        super.put(name, value, ttl, timeUnit);
    }
}