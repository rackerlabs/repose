package com.rackspace.papi.service.datastore;

import java.util.Collection;

/**
 * DatastoreService - service that manages local and distributed Datastores
 */
public interface DatastoreService {

    /**
     * Returns the default Datastore that has been registered with the DatastoreService.
     */
    DatastoreManager defaultDatastore();

    /**
     * Returns the DatastoreManager associated with the provided datastoreName.
     * @param datastoreName
     * @return DatastoreManager or null if one does not exist for the given datastoreName
     */
    DatastoreManager getDatastore(String datastoreName);

    /**
     * Returns a collection of DatastoreManagers of local datastores.  Local datastores are
     * only available on the local node.
     * @return
     */
    Collection<DatastoreManager> availableLocalDatastores();

    /**
     * Returns a collection of DatastoreManagers of distributed datastores.  Distributed
     * datastores are accessible across nodes.
     * @return
     */
    Collection<DatastoreManager> availableDistributedDatastores();

    /**
     * Unregister a DatastoreManager with a given datastoreName.  The DatastoreManager should no longer
     * be accessible from a DatastoreService.
     * @param datastoreName
     */
    void unregisterDatastoreManager(String datastoreName);

    /**
     * Register a DatastoreManager with a given datastoreName.  Once registered, this DatastoreManager
     * should be accessible from the DatastoreService.
     * @param datastoreName
     * @param manager
     */
    void registerDatastoreManager(String datastoreName, DatastoreManager manager);
}
