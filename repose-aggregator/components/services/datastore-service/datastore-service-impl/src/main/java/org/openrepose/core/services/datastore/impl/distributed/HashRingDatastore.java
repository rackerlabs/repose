/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.datastore.impl.distributed;

import org.openrepose.commons.utils.encoding.EncodingProvider;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.datastore.DatastoreOperationException;
import org.openrepose.core.services.datastore.Patch;
import org.openrepose.core.services.datastore.distributed.ClusterView;
import org.openrepose.core.services.datastore.distributed.DistributedDatastore;
import org.openrepose.core.services.datastore.distributed.RemoteBehavior;
import org.openrepose.core.services.datastore.distributed.SerializablePatch;
import org.openrepose.core.services.datastore.hash.MessageDigestFactory;
import org.openrepose.core.services.datastore.impl.distributed.remote.RemoteCommandExecutor;
import org.openrepose.core.services.datastore.impl.distributed.remote.RemoteConnectionException;
import org.openrepose.core.services.datastore.impl.distributed.remote.command.Delete;
import org.openrepose.core.services.datastore.impl.distributed.remote.command.Get;
import org.openrepose.core.services.datastore.impl.distributed.remote.command.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public class HashRingDatastore implements DistributedDatastore {

    public static final String DATASTORE_NAME = "distributed/hash-ring";
    private static final Logger LOG = LoggerFactory.getLogger(HashRingDatastore.class);
    private static final int DEFAULT_TTL = 5;

    private final ClusterView clusterView;
    private final Datastore localDatastore;
    private final RemoteCommandExecutor remoteCommandExecutor;
    private final Object lock = new Object();
    private final EncodingProvider encodingProvider;
    private final MessageDigestFactory hashProvider;
    private final String datasetPrefix;
    private final String name;
    private final String connPoolId;
    private boolean useHttps = false;
    private boolean clusterMemberWarning = false;

    public HashRingDatastore(RemoteCommandExecutor remoteCommandExecutor, ClusterView clusterView,
                             String datastorePrefix, Datastore localDatastore, MessageDigestFactory hashProvider,
                             EncodingProvider encodingProvider, String connPoolId, boolean useHttps) {

        this.name = DATASTORE_NAME;
        this.encodingProvider = encodingProvider;
        this.hashProvider = hashProvider;
        this.datasetPrefix = datastorePrefix;
        this.connPoolId = connPoolId;
        this.useHttps = useHttps;

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
        if (target == null) {
            return false;
        }

        try {
            if (localDatastore == null) {
                return !clusterView.isLocal(target);
            }

            synchronized (lock) {
                Boolean isLocal = (Boolean) localDatastore.get(target.toString());
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
        if (initialBehavior != RemoteBehavior.DISALLOW_FORWARDING) {
            return performRemote(name, id, action, initialBehavior);
        } else {
            LOG.debug("Forwarding for " + action.toString() + " datastore action has been disabled by request.");
        }

        return action.performLocal(name);
    }

    private Object performRemote(String name, byte[] id, DatastoreAction action, RemoteBehavior initialBehavior) {
        boolean targetIsRemote = true;
        RemoteBehavior remoteBehavior =
                clusterView.hasDamagedMembers() ? RemoteBehavior.DISALLOW_FORWARDING : initialBehavior;

        do {
            final InetSocketAddress target = getTarget(id);

            try {
                targetIsRemote = isRemoteTarget(target);
                if (targetIsRemote) {
                    LOG.debug("Routing datastore " + action.toString() + " request for, \"" + name + "\" to: " +
                            target.toString());

                    return action.performRemote(name, target, remoteBehavior);
                }
            } catch (RemoteConnectionException | DatastoreOperationException e) {
                LOG.trace("Could not perform action", e);
                clusterView.memberDamaged(target, e.getMessage());
                remoteBehavior = RemoteBehavior.DISALLOW_FORWARDING;
            }
        } while (targetIsRemote);

        return action.performLocal(name);
    }

    private byte[] getHash(String key) {
        final byte[] stringBytes = (datasetPrefix + key).getBytes(StandardCharsets.UTF_8);

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
        return (Serializable) performAction(hashedKey, id, new DatastoreAction() {

            @Override
            public Object performRemote(String name, InetSocketAddress target, RemoteBehavior remoteBehavior) {
                return remoteCommandExecutor.execute(new Get(name, target, connPoolId, useHttps), remoteBehavior);
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
                return remoteCommandExecutor.execute(new Put(timeUnit, value, ttl, name, target, connPoolId, useHttps), remoteBehavior);
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
                return remoteCommandExecutor.execute(new Delete(name, target, connPoolId, useHttps), remoteBehavior);
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

        return patch(encodingProvider.encode(keyHash), keyHash, (SerializablePatch) patch, ttl, timeUnit, RemoteBehavior.ALLOW_FORWARDING);
    }

    @Override
    public Serializable patch(String hashedKey, byte[] id, final SerializablePatch patch, final int ttl, final TimeUnit timeUnit,
                              RemoteBehavior remoteBehavior) {
        return (Serializable) performAction(hashedKey, id, new DatastoreAction() {

            @Override
            public Object performRemote(String name, InetSocketAddress target, RemoteBehavior remoteBehavior) {
                return remoteCommandExecutor.execute(new org.openrepose.core.services.datastore.impl.distributed.remote.command.Patch(timeUnit, patch, ttl, name, target, connPoolId, useHttps), remoteBehavior);
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
