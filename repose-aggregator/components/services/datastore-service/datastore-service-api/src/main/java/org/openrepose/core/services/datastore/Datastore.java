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
package org.openrepose.core.services.datastore;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * An interface for a Datastore.
 * <p/>
 * A Datastore is used to store and retrieve data.
 */
public interface Datastore {

    /**
     * Gets the value associated with the key from the Datastore.
     * If no value is found will return null.
     *
     * @param key Key provided when the data was stored
     * @return stored value or null
     * @throws DatastoreOperationException if an exception occurs when attempting to retrieve the
     *                                     stored value
     */
    Serializable get(String key) throws DatastoreOperationException;

    /**
     * Put an element in the datastore.
     * If the element already exists in the datastore, then it should be replaced with the
     * value provided.
     *
     * @param key   Identifier for the value being stored
     * @param value The value being stored
     * @throws DatastoreOperationException if an exception occurs when attempting to store the value
     */
    void put(String key, Serializable value) throws DatastoreOperationException;

    /**
     * Put an element in the datastore for a duration of time not to exceed the TimeUnit and duration
     * specified.
     * <p/>
     * If the element already exists in the datastore, then it should be replaced with the
     * value provided.
     *
     * @param key      Identifier for the value being stored
     * @param value    The value being stored
     * @param ttl      Duration (time to live) to store the value for
     * @param timeUnit unit of time {@link java.util.concurrent.TimeUnit} that the ttl is defined in
     * @throws DatastoreOperationException if an exception occurs when attempting to store the value
     */
    void put(String key, Serializable value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException;

    /**
     * Patch (update) an element in the datastore.
     * If the element already exists in the datastore, then it should be updated with the new value (not a full
     * replacement).
     *
     * @param key   Identifier for the value being patched
     * @param patch The patch to apply to the existing value
     * @return the patched and stored value
     * @throws DatastoreOperationException if an exception occurs when attempting to store the value
     */
    <T extends Patchable<T, P>, P extends Patch<T>> T patch(String key, P patch) throws DatastoreOperationException;

    /**
     * Patch (update) an element in the datastore for a duration of time not to exceed the TimeUnit and duration
     * specified.
     * <p/>
     * If the element already exists in the datastore, then it should be updated with the new value (not a full
     * replacement).
     *
     * @param key      Identifier for the value being patched (updated)
     * @param patch    The patch to apply to the existing value
     * @param ttl      Duration (time to live) to store the value for
     * @param timeUnit unit of time {@link java.util.concurrent.TimeUnit} that the ttl is defined in
     * @return the patched and stored value
     * @throws DatastoreOperationException if an exception occurs when attempting to store the value
     */
    <T extends Patchable<T, P>, P extends Patch<T>> T patch(String key, P patch, int ttl, TimeUnit timeUnit) throws DatastoreOperationException;

    /**
     * Removes a value from the Datastore.
     *
     * @param key
     * @return true if the element was removed, false if it was not found in the Datastore
     * @throws DatastoreOperationException if an exception occurs when attempting to remove the
     *                                     stored value
     */
    boolean remove(String key) throws DatastoreOperationException;

    /**
     * Remove all previously stored elements from the datastore
     */
    void removeAll();

    /**
     * Get the name of this datastore
     */
    String getName();
}
