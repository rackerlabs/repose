package com.rackspace.papi.service.datastore;

public interface DatastoreManager {

    Datastore getDatastore(String key);
        
    boolean isDistributed();
}
