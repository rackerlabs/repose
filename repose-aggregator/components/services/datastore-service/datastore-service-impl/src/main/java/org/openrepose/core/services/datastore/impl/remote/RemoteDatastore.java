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
package org.openrepose.core.services.datastore.impl.remote;

import org.openrepose.commons.utils.encoding.EncodingProvider;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.datastore.DatastoreOperationException;
import org.openrepose.core.services.datastore.Patch;
import org.openrepose.core.services.datastore.distributed.DistributedDatastore;
import org.openrepose.core.services.datastore.distributed.RemoteBehavior;
import org.openrepose.core.services.datastore.distributed.SerializablePatch;
import org.openrepose.core.services.datastore.hash.MessageDigestFactory;
import org.openrepose.core.services.datastore.impl.distributed.DatastoreAction;
import org.openrepose.core.services.datastore.impl.distributed.remote.RemoteCommandExecutor;
import org.openrepose.core.services.datastore.impl.distributed.remote.RemoteConnectionException;
import org.openrepose.core.services.datastore.impl.distributed.remote.command.Delete;
import org.openrepose.core.services.datastore.impl.distributed.remote.command.Get;
import org.openrepose.core.services.datastore.impl.distributed.remote.command.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public class RemoteDatastore implements DistributedDatastore {

    public static final String DATASTORE_NAME = "distributed/remote";
    private static final Logger LOG = LoggerFactory.getLogger(RemoteDatastore.class);
    private static final int DEFAULT_TTL = 5;

    private final Datastore localDatastore;
    private final RemoteCommandExecutor remoteCommandExecutor;
    private final EncodingProvider encodingProvider;
    private final MessageDigestFactory hashProvider;
    private final String datasetPrefix;
    private final String name;
    private final InetSocketAddress target;
    private final String connPoolId;
    private final boolean useHttps;

    public RemoteDatastore(RemoteCommandExecutor remoteCommandExecutor,
                           String datastorePrefix, Datastore localDatastore, MessageDigestFactory hashProvider,
                           EncodingProvider encodingProvider, InetSocketAddress target, String connPoolId, boolean useHttps) {

        this.name = DATASTORE_NAME;
        this.encodingProvider = encodingProvider;
        this.hashProvider = hashProvider;
        this.datasetPrefix = datastorePrefix;
        this.target = target;
        this.connPoolId = connPoolId;
        this.useHttps = useHttps;

        this.localDatastore = localDatastore;
        this.remoteCommandExecutor = remoteCommandExecutor;
        if (localDatastore == null) {
            LOG.warn("Local datastore is null");
        }
    }

    private Object performAction(String name, DatastoreAction action) {
        return performRemote(name, action);
    }

    private Object performRemote(String name, DatastoreAction action) {
        try {
            LOG.debug("Routing datastore " + action.toString() + " request for, \"" + name + "\" to: " + target.toString());
            return action.performRemote(name, target, RemoteBehavior.ALLOW_FORWARDING);
        } catch (RemoteConnectionException | DatastoreOperationException e) {
            LOG.trace("Could not perform action", e);
        }
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
    public Serializable get(String key) {
        final byte[] keyHash = getHash(key);

        return get(encodingProvider.encode(keyHash), keyHash, RemoteBehavior.ALLOW_FORWARDING);
    }

    @Override
    public Serializable get(String hashedKey, byte[] id, RemoteBehavior remoteBehavior) {
        return (Serializable) performAction(hashedKey, new DatastoreAction() {

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
        });
    }

    @Override
    public void put(String key, Serializable value) {
        put(key, value, DEFAULT_TTL, TimeUnit.MINUTES);
    }

    @Override
    public void put(String key, Serializable value, int ttl, TimeUnit timeUnit) {
        final byte[] keyHash = getHash(key);

        put(encodingProvider.encode(keyHash), keyHash, value, ttl, timeUnit, RemoteBehavior.ALLOW_FORWARDING);
    }

    @Override
    public void put(String hashedKey, byte[] id, final Serializable value, final int ttl, final TimeUnit timeUnit,
                    RemoteBehavior remoteBehavior) {
        performAction(hashedKey, new DatastoreAction() {

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
        });
    }

    @Override
    public boolean remove(String key) {
        final byte[] keyHash = getHash(key);

        return remove(encodingProvider.encode(keyHash), keyHash, RemoteBehavior.ALLOW_FORWARDING);
    }

    @Override
    public boolean remove(String hashedKey, byte[] id, RemoteBehavior remoteBehavior) {
        return (Boolean) performAction(hashedKey, new DatastoreAction() {

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
        });
    }

    @Override
    public Serializable patch(String key, Patch patch) {
        return patch(key, patch, DEFAULT_TTL, TimeUnit.MINUTES);
    }

    @Override
    public Serializable patch(String key, Patch patch, int ttl, TimeUnit timeUnit) {
        final byte[] keyHash = getHash(key);

        return patch(encodingProvider.encode(keyHash), keyHash, (SerializablePatch) patch, ttl, timeUnit, RemoteBehavior.ALLOW_FORWARDING);
    }

    @Override
    public Serializable patch(String hashedKey, byte[] id, final SerializablePatch patch, final int ttl, final TimeUnit timeUnit,
                              RemoteBehavior remoteBehavior) {
        return (Serializable) performAction(hashedKey, new DatastoreAction() {

            @Override
            public Object performRemote(String name, InetSocketAddress target, RemoteBehavior remoteBehavior) {
                return remoteCommandExecutor.execute(
                        new org.openrepose.core.services.datastore.impl.distributed.remote.command.Patch(
                                timeUnit, patch, ttl, name, target, connPoolId, useHttps),
                        remoteBehavior);
            }

            @Override
            public Object performLocal(String name) {
                return localDatastore.patch(name, patch, ttl, timeUnit);
            }

            @Override
            public String toString() {
                return "patch";
            }
        });
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
