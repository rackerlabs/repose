package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.DatastoreUnavailableException;


public final class AvailablityGuard implements DatastoreManager {

    private final DatastoreManager manager;

    public AvailablityGuard(DatastoreManager manager) {
        this.manager = manager;
    }

    @Override
    public String getName() {
        return manager.getName();
    }

    @Override
    public void destroy() {
        manager.destroy();
    }

    @Override
    public boolean isDistributed() {
        return manager.isDistributed();
    }

    @Override
    public boolean isAvailable() {
        return manager.isAvailable();
    }

    @Override
    public Datastore getDatastore() throws DatastoreUnavailableException {
        if (!isAvailable()) {
            throw new DatastoreUnavailableException("Datastore  " + manager.toString() + " is currently unavailable.");
        }

        return manager.getDatastore();
    }
}
