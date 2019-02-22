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
package org.openrepose.core.services.datastore.distributed;

import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.datastore.DatastoreOperationException;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * An interface for a Distributed Datastore that extends the behavior of a Datastore.
 * Distributed datastores allow retrieval and storage of data across local and remote datastores.
 */
public interface DistributedDatastore extends Datastore {

    /**
     * Put an element in a local or remote datastore for a duration of time not to exceed the TimeUnit and duration
     * specified.
     * <p/>
     * If the element already exists in the datastore, then it should be replaced with the
     * value provided.
     *
     * @param key            Identifier for the value being stored
     * @param id             Identifier for the cluster member where this value should be stored
     * @param value          The value being stored
     * @param ttl            Duration to store the value for
     * @param timeUnit       Unit of time {@link java.util.concurrent.TimeUnit} that the ttl is defined in
     * @param remoteBehavior Whether or not to allow remote storage of this value
     * @throws org.openrepose.core.services.datastore.DatastoreOperationException if an exception occurs when
     *                                                                            attempting to store the value
     */
    void put(String key, byte[] id, final Serializable value, final int ttl, final TimeUnit timeUnit,
             RemoteBehavior remoteBehavior) throws DatastoreOperationException;

    /**
     * Patch (update) an element in a local or remote datastore for a duration of time not to exceed the TimeUnit and
     * duration specified.
     * <p/>
     * If the element already exists in the datastore, then it should update based on the value provided
     * (not a complete replacement).
     *
     * @param key            Identifier for the value being patched (update)
     * @param id             Identifier for the cluster member where this value should be patched (update)
     * @param patch          The patch to be applied to the stored value
     * @param ttl            Duration to store the value for
     * @param timeUnit       unit of time {@link java.util.concurrent.TimeUnit} that the ttl is defined in
     * @param remoteBehavior Whether or not to allow remote storage of this value
     * @return the patched and stored value
     * @throws org.openrepose.core.services.datastore.DatastoreOperationException if an exception occurs when
     *                                                                            attempting to store the value
     */
    Serializable patch(String key, byte[] id, final SerializablePatch patch, final int ttl, final TimeUnit timeUnit,
                       RemoteBehavior remoteBehavior) throws DatastoreOperationException;

    /**
     * Removes an object from the Datastore.
     *
     * @param key
     * @param id             Identifier for the cluster member where this value should be stored
     * @param remoteBehavior Whether or not to allow remote removal of this value
     * @return True if the element was removed, false if it was not found in the Datastore
     * @throws DatastoreOperationException if an exception occurs when attempting to remove the
     *                                     stored value
     */
    boolean remove(String key, byte[] id, RemoteBehavior remoteBehavior) throws DatastoreOperationException;

    /**
     * Gets a value from the Datastore.  If there is no value found for the
     * provided key, a null object should be returned.
     *
     * @param key            Key provided when the data was stored
     * @param id             Identifier for the cluster member where this value should be stored
     * @param remoteBehavior Whether or not to allow remote retrieval of this value
     * @return The stored value
     * @throws DatastoreOperationException if an exception occurs when attempting to retrieve the
     *                                     stored value
     */
    Serializable get(String key, byte[] id, RemoteBehavior remoteBehavior) throws DatastoreOperationException;

}
