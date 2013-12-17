package com.rackspace.papi.service.datastore;

import com.rackspace.papi.components.datastore.Datastore;
import com.rackspace.papi.components.datastore.DatastoreUnavailableException;
import com.rackspace.papi.components.datastore.distributed.DistDatastoreConfiguration;
import com.rackspace.papi.components.datastore.distributed.DistributedDatastore;

/**
 * DatastoreService - service that manages the lifecycle and configuration of {@link com.rackspace.papi.components.datastore.Datastore}s
 */
public interface DatastoreService {

    /**
     * Get the default datastore
     */
    Datastore getDefaultDatastore() throws DatastoreUnavailableException;

    /**
     * Get a datastore associated with the provided datastore name
     * @param datastoreName
     * @return
     * @throws DatastoreUnavailableException if no datastore exists with the given datastoreName
     */
    Datastore getDatastore(String datastoreName) throws DatastoreUnavailableException;

    /**
     * Get the distributed datastore managed by the service.
     * @return
     * @throws DatastoreUnavailableException if no distributed datastore exists
     */
    DistributedDatastore getDistributedDatastore() throws DatastoreUnavailableException;

    /**
     * Shutdown the datastore associated with the datastore name
     * @param datastoreName
     */
    void destroyDatastore(String datastoreName);

    /**
     * Create and return a distributed datastore using the provided configuration.  The created
     * datastore can be retrieved by the same name provided using getDatastore(datastoreName)
     * @param datastoreName
     * @param configuration
     * @return
     * @throws DatastoreServiceException if the datastore creation fails
     */
    DistributedDatastore createDatastore(String datastoreName, DistDatastoreConfiguration configuration)
            throws DatastoreServiceException;
}
