package com.rackspace.papi.service.datastore;

import com.rackspace.papi.service.datastore.distributed.DistDatastoreConfiguration;
import com.rackspace.papi.service.datastore.distributed.DistributedDatastore;

public interface DatastoreService {

    /**
     * Always returns the default, local (read L1) cache that has been registered
     * with the datastore service.
     */
    Datastore getDefaultDatastore() throws DatastoreUnavailableException;

    Datastore getDatastore(String datastoreName) throws DatastoreUnavailableException;

    DistributedDatastore getDistributedDatastore() throws DatastoreUnavailableException;

    void destroyDatastore(String datastoreName);

    DistributedDatastore createDatastore(String datastoreName, DistDatastoreConfiguration configuration)
            throws DatastoreServiceException;
}
