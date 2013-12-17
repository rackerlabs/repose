package com.rackspace.papi.components.datastore;

import java.util.concurrent.TimeUnit;

/**
 * An interface for a Datastore.
 *
 * A Datastore is used to store and retrieve data.
 */
public interface Datastore {

    String DEFAULT_LOCAL = "local/default";

    /**
     * Gets the value associated with the key from the Datastore.  If there is no value found for the
     * provided key, a StoredElement object should be returned containing a null or empty element.
     *
     * @param key Key provided when the data was stored
     * @return StoredElement wrapper of the stored value
     * @throws DatastoreOperationException if an exception occurs when attempting to retrieve the
     * stored value
     */
    StoredElement get(String key) throws DatastoreOperationException;

    /**
     * Put an element in the datastore.
     * <p/>
     * <li>if the element already exists in the datastore, then ...
     * </ul>
     *
     * @param key Identifier for the value being stored
     * @param value The value being stored
     * @throws DatastoreOperationException if an exception occurs when attempting to store the value
     */
    void put(String key, byte[] value) throws DatastoreOperationException;

    /**
     * Put an element in the datastore for a duration of time not to exceed the TimeUnit and duration
     * specified.
     * <p/>
     * <li>if the element already exists in the datastore, then ...
     * </ul>
     *
     * @param key Identifier for the value being stored
     * @param value The value being stored
     * @param ttl Duration (time to live) to store the value for
     * @param timeUnit unit of time {@link java.util.concurrent.TimeUnit} that the ttl is defined in
     * @throws DatastoreOperationException if an exception occurs when attempting to store the value
     */
    void put(String key, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException;

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
    void removeAllCacheData();

    /**
     * Get the name of this datastore
     */
    String getName();
}
