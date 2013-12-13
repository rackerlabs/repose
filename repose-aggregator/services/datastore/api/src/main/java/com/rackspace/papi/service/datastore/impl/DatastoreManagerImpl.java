package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreUnavailableException;


public final class DatastoreManagerImpl implements DatastoreManager {

    private final DatastoreManager manager;

    public DatastoreManagerImpl(DatastoreManager manager) {
        this.manager = manager;
    }

    @Override
    public Datastore getDatastore() throws DatastoreUnavailableException {
        final Datastore datastore = manager.getDatastore();

        if (datastore == null) {
            throw new DatastoreUnavailableException("Datastore  " + manager.toString() + " is currently unavailable.");
        }

        return datastore;
    }

    @Override
    public boolean isDistributed() {
        return manager.isDistributed();
    }

    @Override
    public void destroy() {
        manager.destroy();
    }
}
