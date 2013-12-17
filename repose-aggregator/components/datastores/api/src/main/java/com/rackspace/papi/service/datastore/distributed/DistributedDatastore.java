package com.rackspace.papi.service.datastore.distributed;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.StoredElement;

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
     * <li>if the element already exists in the datastore, then ...
     * </ul>
     *
     * @param key Identifier for the value being stored
     * @param id ???
     * @param value The value being stored
     * @param ttl Duration to store the value for
     * @param timeUnit unit of time {@link java.util.concurrent.TimeUnit} that the ttl is defined in
     * @param allowForwarding Whether or not to allow remote storage of this value
     * @throws com.rackspace.papi.service.datastore.DatastoreOperationException if an exception occurs when
     *         attempting to store the value
     */
    public void put(String key, byte[] id, final byte[] value, final int ttl, final TimeUnit timeUnit,
                    RemoteBehavior allowForwarding) throws DatastoreOperationException;

    /**
     * Removes a StoredElement from the Datastore.
     *
     * @param key
     * @param id ???
     * @param allowForwarding Whether or not to allow remote removal of this value
     * @return true if the element was removed, false if it was not found in the Datastore
     * @throws DatastoreOperationException if an exception occurs when attempting to remove the
     * stored value
     */
    public boolean remove(String key, byte[] id, RemoteBehavior allowForwarding);

    /**
     * Gets a StoredElement from the Datastore.  If there is no value found for the
     * provided key, a StoredElement object should be returned containing a null or empty element.
     *
     * @param key Key provided when the data was stored
     * @param id ???
     * @param allowForwarding Whether or not to allow remote retrieval of this value
     * @return StoredElement wrapper of the stored value
     * @throws DatastoreOperationException if an exception occurs when attempting to retrieve the
     * stored value
     */
    public StoredElement get(String key, byte[] id, RemoteBehavior allowForwarding);

}
