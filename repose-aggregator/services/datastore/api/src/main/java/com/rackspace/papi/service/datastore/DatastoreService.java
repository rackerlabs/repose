package com.rackspace.papi.service.datastore;

public interface DatastoreService {

    /**
     * Always returns the default, local (read L1) cache that has been registered
     * with the datastore service.
     */
    Datastore getDefaultDatastore();

    Datastore getDatastore(String datastoreName);

    DistributedDatastore getDistributedDatastore();

    void destroyDatastore(String datastoreName);

    DistributedDatastore createDatastore(String datastoreName, DistDatastoreConfiguration configuration);

    /**
     * DEPRECATED: ReplicatedDatastoreFilter is currently using this method.  Once that is removed,
     * we can remove this from the interface
     * @param datastoreManagerName
     * @param manager
     */
    void registerDatastoreManager(String datastoreManagerName, DatastoreManager manager);
}
