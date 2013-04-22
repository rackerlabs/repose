package com.rackspace.papi.service.datastore.impl.distributed.hash;

import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.impl.distributed.common.RemoteBehavior;
import com.rackspace.papi.service.datastore.impl.distributed.hash.remote.RemoteCommandExecutor;
import com.rackspace.papi.service.datastore.impl.distributed.hash.remote.RemoteConnectionException;
import com.rackspace.papi.service.datastore.impl.distributed.hash.remote.command.Delete;
import com.rackspace.papi.service.datastore.impl.distributed.hash.remote.command.Get;
import com.rackspace.papi.service.datastore.impl.distributed.hash.remote.command.Put;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.encoding.EncodingProvider;
import com.rackspace.papi.service.datastore.hash.MessageDigestFactory;
import com.rackspace.papi.service.datastore.impl.AbstractHashedDatastore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

public class HashRingDatastore extends AbstractHashedDatastore {

    private static final int ADDRESS_TTL_MINUTES = 5;
    private static final Logger LOG = LoggerFactory.getLogger(HashRingDatastore.class);
    private final MutableClusterView clusterView;
    private final Datastore localDatastore;
    private final RemoteCommandExecutor remoteCommandExecutor;
    private boolean clusterMemberWarning = false;
    private final Object lock = new Object();

    public HashRingDatastore(RemoteCommandExecutor remoteCommandExecutor, MutableClusterView clusterView,
            String datastorePrefix, Datastore localDatastore, MessageDigestFactory hashProvider,
            EncodingProvider encodingProvider) {
        super(datastorePrefix, encodingProvider, hashProvider);

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
                        localDatastore.put(target.toString(), bytes, ADDRESS_TTL_MINUTES, TimeUnit.MINUTES);
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
        boolean targetIsRemote = false;

        if (initialBehavior != RemoteBehavior.DISALLOW_FORWARDING) {
            RemoteBehavior remoteBehavior =
                    clusterView.hasDamagedMembers() ? RemoteBehavior.DISALLOW_FORWARDING : initialBehavior;

            do {
                final InetSocketAddress target = getTarget(id);

                if (target != null && (targetIsRemote = isRemoteTarget(target))) {
                    LOG.debug("Routing datastore " + action.toString() + " request for, \"" + name + "\" to: " +
                                      target.toString());

                    try {
                        return action.performRemote(name, target, remoteBehavior);
                    } catch (RemoteConnectionException rce) {
                        clusterView.memberDamaged(target, rce.getMessage());
                        remoteBehavior = RemoteBehavior.DISALLOW_FORWARDING;
                    }
                }
            } while (targetIsRemote);
        } else {
            LOG.debug("Forwarding for " + action.toString() + " datastore action has been disabled by request.");
        }

        return action.performLocal(name);
    }

    @Override
    protected StoredElement get(String name, byte[] id) {
        return get(name, id, RemoteBehavior.ALLOW_FORWARDING);
    }

    @Override
    protected boolean remove(String name, byte[] id) {
        return remove(name, id, RemoteBehavior.ALLOW_FORWARDING);
    }

    @Override
    protected void put(String name, byte[] id, final byte[] value, final int ttl, final TimeUnit timeUnit) {
        put(name, id, value, ttl, timeUnit, RemoteBehavior.ALLOW_FORWARDING);
    }

    public StoredElement get(String name, byte[] id, RemoteBehavior initialBehavior) {
        return (StoredElement) performAction(name, id, new DatastoreAction() {

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
        }, initialBehavior);
    }

    public boolean remove(String name, byte[] id, RemoteBehavior initialBehavior) {
        return (Boolean) performAction(name, id, new DatastoreAction() {

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
        }, initialBehavior);
    }

    public void put(String name, byte[] id, final byte[] value, final int ttl, final TimeUnit timeUnit,
            RemoteBehavior initialBehavior) {
        performAction(name, id, new DatastoreAction() {

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
        }, initialBehavior);
    }

    @Override
    public boolean remove(String key, boolean notify) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void put(String key, byte[] value, boolean notify) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void put(String key, byte[] value, int ttl, TimeUnit timeUnit, boolean notify) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void removeAllCachedData() {
        localDatastore.removeAllCacheData();
    }
}