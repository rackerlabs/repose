package com.rackspace.papi.components.datastore.impl.distributed;

import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.commons.util.io.charset.CharacterSets;
import com.rackspace.papi.components.datastore.Datastore;
import com.rackspace.papi.components.datastore.DatastoreOperationException;
import com.rackspace.papi.components.datastore.StoredElement;
import com.rackspace.papi.components.datastore.distributed.ClusterView;
import com.rackspace.papi.commons.util.encoding.EncodingProvider;
import com.rackspace.papi.components.datastore.distributed.DistributedDatastore;
import com.rackspace.papi.components.datastore.hash.MessageDigestFactory;
import com.rackspace.papi.components.datastore.distributed.RemoteBehavior;
import com.rackspace.papi.components.datastore.impl.distributed.remote.RemoteCommandExecutor;
import com.rackspace.papi.components.datastore.impl.distributed.remote.RemoteConnectionException;
import com.rackspace.papi.components.datastore.impl.distributed.remote.command.Delete;
import com.rackspace.papi.components.datastore.impl.distributed.remote.command.Get;
import com.rackspace.papi.components.datastore.impl.distributed.remote.command.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public class HashRingDatastore implements DistributedDatastore {

    public static final String DATASTORE_NAME = "distributed/hash-ring";
    private static final Logger LOG = LoggerFactory.getLogger(HashRingDatastore.class);
    private static final int DEFAULT_TTL = 5;

    private final ClusterView clusterView;
    private final Datastore localDatastore;
    private final RemoteCommandExecutor remoteCommandExecutor;
    private boolean clusterMemberWarning = false;
    private final Object lock = new Object();

    private final EncodingProvider encodingProvider;
    private final MessageDigestFactory hashProvider;
    private final String datasetPrefix;
    private final String name;

    public HashRingDatastore(RemoteCommandExecutor remoteCommandExecutor, ClusterView clusterView,
            String datastorePrefix, Datastore localDatastore, MessageDigestFactory hashProvider,
            EncodingProvider encodingProvider) {

        this.name = DATASTORE_NAME;
        this.encodingProvider = encodingProvider;
        this.hashProvider = hashProvider;
        this.datasetPrefix = datastorePrefix;

        this.clusterView = clusterView;
        this.localDatastore = localDatastore;
        this.remoteCommandExecutor = remoteCommandExecutor;
        if (localDatastore == null) {
            LOG.warn("Local datastore is null");
        }
    }

    public InetSocketAddress getTarget(byte[] hashBytes) {
        final InetSocketAddress[] ringMembers = clusterView.members();

        if (ringMembers.length <= 0) {
            if (!clusterMemberWarning) {
                LOG.info("No members to route to in datastore cluster... unable to route this datastore request.");
                clusterMemberWarning = true;
            }

            return null;
        }

        clusterMemberWarning = false;
        final int memberAddress =
                new BigInteger(hashBytes).mod(BigInteger.valueOf(ringMembers.length)).abs().intValue();
        return ringMembers[memberAddress];
    }

    private boolean isRemoteTarget(InetSocketAddress target) {
        try {

            if (localDatastore == null) {
                return !clusterView.isLocal(target);
            }

            synchronized (lock) {
                StoredElement value = localDatastore.get(target.toString());
                Boolean isLocal = value != null && !value.elementIsNull() ? value.elementAs(Boolean.class) : null;
                if (isLocal == null) {
                    isLocal = clusterView.isLocal(target);
                    byte[] bytes;
                    try {
                        bytes = ObjectSerializer.instance().writeObject(isLocal);
                        localDatastore.put(target.toString(), bytes, DEFAULT_TTL, TimeUnit.MINUTES);
                    } catch (IOException ex) {
                        LOG.warn("Unable to cache target address", ex);
                    }
                }
                return !isLocal;
            }
        } catch (SocketException se) {
            throw new DatastoreOperationException("Unable to identify local cluster target datastore", se);
        }
    }

    private Object performAction(String name, byte[] id, DatastoreAction action, RemoteBehavior initialBehavior) {
        boolean targetIsRemote = true;

        if (initialBehavior != RemoteBehavior.DISALLOW_FORWARDING) {
            RemoteBehavior remoteBehavior =
                    clusterView.hasDamagedMembers() ? RemoteBehavior.DISALLOW_FORWARDING : initialBehavior;

            do {
                final InetSocketAddress target = getTarget(id);

                try {
                    if (target == null) {
                        targetIsRemote = false;
                    } else if (targetIsRemote = isRemoteTarget(target)) {
                        LOG.debug("Routing datastore " + action.toString() + " request for, \"" + name + "\" to: " +
                                target.toString());

                        return action.performRemote(name, target, remoteBehavior);
                    }
                } catch (RemoteConnectionException rce) {
                    clusterView.memberDamaged(target, rce.getMessage());
                    remoteBehavior = RemoteBehavior.DISALLOW_FORWARDING;
                } catch (DatastoreOperationException doe) {
                    clusterView.memberDamaged(target, doe.getMessage());
                    remoteBehavior = RemoteBehavior.DISALLOW_FORWARDING;
                }
            } while (targetIsRemote);
        } else {
            LOG.debug("Forwarding for " + action.toString() + " datastore action has been disabled by request.");
        }

        return action.performLocal(name);
    }


    private byte[] getHash(String key) {
        final byte[] stringBytes = (datasetPrefix + key).getBytes(CharacterSets.UTF_8);

        try {
            return hashProvider.newMessageDigest().digest(stringBytes);
        } catch (NoSuchAlgorithmException algorithmException) {
            throw new DatastoreOperationException("Failed to hash key. Reason: " + algorithmException.getMessage(),
                    algorithmException);
        }
    }

    @Override
    public StoredElement get(String key) throws DatastoreOperationException {
        final byte[] keyHash = getHash(key);

        return get(encodingProvider.encode(keyHash), keyHash, RemoteBehavior.ALLOW_FORWARDING);
    }

    @Override
    public StoredElement get(String key, byte[] id, RemoteBehavior remoteBehavior) {
        return (StoredElement) performAction(key, id, new DatastoreAction() {

            @Override
            public Object performRemote(String name, InetSocketAddress target, RemoteBehavior remoteBehavior) {
                return remoteCommandExecutor.execute(new Get(name, target), remoteBehavior);
            }

            @Override
            public Object performLocal(String name) {
                return localDatastore.get(name);
            }

            @Override
            public String toString() {
                return "get";
            }
        }, remoteBehavior);
    }

    @Override
    public boolean remove(String key) throws DatastoreOperationException {
        final byte[] keyHash = getHash(key);

        return remove(encodingProvider.encode(keyHash), keyHash, RemoteBehavior.ALLOW_FORWARDING);
    }

    @Override
    public boolean remove(String key, byte[] id, RemoteBehavior remoteBehavior) {
        return (Boolean) performAction(key, id, new DatastoreAction() {

            @Override
            public Object performRemote(String name, InetSocketAddress target, RemoteBehavior remoteBehavior) {
                return remoteCommandExecutor.execute(new Delete(name, target), remoteBehavior);
            }

            @Override
            public Object performLocal(String name) {
                return localDatastore.remove(name);
            }

            @Override
            public String toString() {
                return "remove";
            }
        }, remoteBehavior);
    }


    @Override
    public void put(String key, byte[] value) throws DatastoreOperationException {
        put(key, value, DEFAULT_TTL, TimeUnit.MINUTES);
    }

    @Override
    public void put(String key, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
        final byte[] keyHash = getHash(key);

        put(encodingProvider.encode(keyHash), keyHash, value, ttl, timeUnit, RemoteBehavior.ALLOW_FORWARDING);
    }

    @Override
    public void put(String key, byte[] id, final byte[] value, final int ttl, final TimeUnit timeUnit,
            RemoteBehavior remoteBehavior) {
        performAction(key, id, new DatastoreAction() {

            @Override
            public Object performRemote(String name, InetSocketAddress target, RemoteBehavior remoteBehavior) {
                return remoteCommandExecutor.execute(new Put(timeUnit, value, ttl, name, target), remoteBehavior);
            }

            @Override
            public Object performLocal(String name) {
                localDatastore.put(name, value, ttl, timeUnit);

                return null;
            }

            @Override
            public String toString() {
                return "put";
            }
        }, remoteBehavior);
    }


    @Override
    public void removeAll() {
        localDatastore.removeAll();
    }

    @Override
    public String getName() {
        return name;
    }

}
