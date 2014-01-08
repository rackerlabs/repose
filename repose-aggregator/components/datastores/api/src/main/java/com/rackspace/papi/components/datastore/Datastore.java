package com.rackspace.papi.components.datastore;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * An interface for a Datastore.
 *
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
     * stored value
     */
    Serializable get(String key) throws DatastoreOperationException;

    /**
     * Put an element in the datastore.
     * If the element already exists in the datastore, then it should be replaced with the
     * value provided.
     *
     * @param key Identifier for the value being stored
     * @param value The value being stored
     * @throws DatastoreOperationException if an exception occurs when attempting to store the value
     */
    void put(String key, Serializable value) throws DatastoreOperationException;

    /**
     * Put an element in the datastore for a duration of time not to exceed the TimeUnit and duration
     * specified.
     *
     * If the element already exists in the datastore, then it should be replaced with the
     * value provided.
     *
     * @param key Identifier for the value being stored
     * @param value The value being stored
     * @param ttl Duration (time to live) to store the value for
     * @param timeUnit unit of time {@link java.util.concurrent.TimeUnit} that the ttl is defined in
     * @throws DatastoreOperationException if an exception occurs when attempting to store the value
     */
    void put(String key, Serializable value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException;

    /**
     * Patch (update) an element in the datastore.
     * If the element already exists in the datastore, then it should be updated with the new value (not a full
     * replacement).
     *
     * @param key Identifier for the value being patched
     * @param patch The patch to apply to the existing value
     * @return StoredElement wrapper of the updated stored value
     * @throws DatastoreOperationException if an exception occurs when attempting to store the value
     */
    StoredElement patch(String key, Patch patch) throws DatastoreOperationException;

    /**
     * Patch (update) an element in the datastore for a duration of time not to exceed the TimeUnit and duration
     * specified.
     *
     * If the element already exists in the datastore, then it should be updated with the new value (not a full
     * replacement).
     *
     * @param key Identifier for the value being patched (updated)
     * @param patch The patch to apply to the existing value
     * @param ttl Duration (time to live) to store the value for
     * @param timeUnit unit of time {@link java.util.concurrent.TimeUnit} that the ttl is defined in
     * @return StoredElement wrapper of the updated stored value
     * @throws DatastoreOperationException if an exception occurs when attempting to store the value
     */
    StoredElement patch(String key, Patch patch, int ttl, TimeUnit timeUnit) throws DatastoreOperationException;

    /**
     * Removes a value from the Datastore.
     *
     * @param key
     * @return true if the element was removed, false if it was not found in the Datastore
     * @throws DatastoreOperationException if an exception occurs when attempting to remove the
     * stored value
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
