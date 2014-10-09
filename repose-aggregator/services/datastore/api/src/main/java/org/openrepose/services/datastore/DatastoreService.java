package org.openrepose.services.datastore;

import org.openrepose.services.datastore.distributed.ClusterConfiguration;
import org.openrepose.services.datastore.distributed.DistributedDatastore;

/**
 * DatastoreService - service that manages the lifecycle and configuration of {@link Datastore}s
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
    DistributedDatastore createDatastore(String datastoreName, ClusterConfiguration configuration)
            throws DatastoreServiceException;

    /**
     * Shutdown all datastores
     */
    void shutdown();
}
