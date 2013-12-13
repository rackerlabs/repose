package com.rackspace.papi.service.datastore;

public interface DatastoreManager {

    Datastore getDatastore() throws DatastoreUnavailableException;

    boolean isDistributed();

    String getName();

    void destroy();
}
