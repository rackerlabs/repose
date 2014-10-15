package org.openrepose.services.datastore.impl.distributed;

import org.openrepose.commons.utils.encoding.EncodingProvider;
import org.openrepose.commons.utils.io.charset.CharacterSets;
import org.openrepose.services.datastore.Datastore;
import org.openrepose.services.datastore.DatastoreOperationException;
import org.openrepose.services.datastore.Patch;
import org.openrepose.services.datastore.distributed.ClusterView;
import org.openrepose.services.datastore.distributed.DistributedDatastore;
import org.openrepose.services.datastore.distributed.RemoteBehavior;
import org.openrepose.services.datastore.distributed.SerializablePatch;
import org.openrepose.services.datastore.hash.MessageDigestFactory;
import org.openrepose.services.datastore.impl.distributed.remote.RemoteCommandExecutor;
import org.openrepose.services.datastore.impl.distributed.remote.RemoteConnectionException;
import org.openrepose.services.datastore.impl.distributed.remote.command.Delete;
import org.openrepose.services.datastore.impl.distributed.remote.command.Get;
import org.openrepose.services.datastore.impl.distributed.remote.command.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
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
                Boolean isLocal = (Boolean)localDatastore.get(target.toString());
                if (isLocal == null) {
                    isLocal = clusterView.isLocal(target);
                    localDatastore.put(target.toString(), isLocal, DEFAULT_TTL, TimeUnit.MINUTES);
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
                    LOG.trace("Could not perform action", rce);
                    clusterView.memberDamaged(target, rce.getMessage());
                    remoteBehavior = RemoteBehavior.DISALLOW_FORWARDING;
                } catch (DatastoreOperationException doe) {
                    LOG.trace("Could not perform action",doe);
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
    public Serializable get(String key) throws DatastoreOperationException {
        final byte[] keyHash = getHash(key);

        return get(encodingProvider.encode(keyHash), keyHash, RemoteBehavior.ALLOW_FORWARDING);
    }

    @Override
    public Serializable get(String hashedKey, byte[] id, RemoteBehavior remoteBehavior) {
        return (Serializable)performAction(hashedKey, id, new DatastoreAction() {

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

    //todo: make this, remove and get do the hash consistently. if we need another method to deal only in pre-hashed versions write it
    @Override
    public void put(String key, Serializable value) throws DatastoreOperationException {
        put(key, value, DEFAULT_TTL, TimeUnit.MINUTES);
    }

    @Override
    public void put(String key, Serializable value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
        final byte[] keyHash = getHash(key);

        put(encodingProvider.encode(keyHash), keyHash, value, ttl, timeUnit, RemoteBehavior.ALLOW_FORWARDING);
    }

    @Override
    public void put(String hashedKey, byte[] id, final Serializable value, final int ttl, final TimeUnit timeUnit,
                    RemoteBehavior remoteBehavior) throws DatastoreOperationException {
        performAction(hashedKey, id, new DatastoreAction() {

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
    public boolean remove(String key) throws DatastoreOperationException {
        final byte[] keyHash = getHash(key);

        return remove(encodingProvider.encode(keyHash), keyHash, RemoteBehavior.ALLOW_FORWARDING);
    }

    @Override
    public boolean remove(String hashedKey, byte[] id, RemoteBehavior remoteBehavior) {
        return (Boolean) performAction(hashedKey, id, new DatastoreAction() {

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
    public Serializable patch(String key, Patch patch) throws DatastoreOperationException {
        return patch(key, patch, DEFAULT_TTL, TimeUnit.MINUTES);
    }

    @Override
    public Serializable patch(String key, Patch patch, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
        final byte[] keyHash = getHash(key);

        return patch(encodingProvider.encode(keyHash), keyHash, (SerializablePatch)patch, ttl, timeUnit, RemoteBehavior.ALLOW_FORWARDING);
    }

    @Override
    public Serializable patch(String hashedKey, byte[] id, final SerializablePatch patch, final int ttl, final TimeUnit timeUnit,
                    RemoteBehavior remoteBehavior) {
        return (Serializable)performAction(hashedKey, id, new DatastoreAction() {

            @Override
            public Object performRemote(String name, InetSocketAddress target, RemoteBehavior remoteBehavior) {
                return remoteCommandExecutor.execute(new org.openrepose.services.datastore.impl.distributed.remote.command.Patch(timeUnit, patch, ttl, name, target), remoteBehavior);
            }

            @Override
            public Object performLocal(String name) {
                return localDatastore.patch(name, patch, ttl, timeUnit);
            }

            @Override
            public String toString() {
                return "patch";
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
