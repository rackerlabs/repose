package com.rackspace.papi.service.datastore;

import javax.naming.NamingException;

public interface DatastoreService {

    public static final String DEFAULT_LOCAL = "local/default";

    /**
     * Always returns the default, local (read L1) cache that has been registered
     * with the datastore service.
     */
    Datastore defaultDatastore();

    Datastore getDatastore(String datastoreName);
    
    void registerDatastoreManager(String datastoreManagerName, DatastoreManager manager) throws NamingException;
}
