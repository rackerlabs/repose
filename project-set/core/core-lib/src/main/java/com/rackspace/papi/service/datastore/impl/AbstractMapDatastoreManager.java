package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import java.util.Map;

public abstract class AbstractMapDatastoreManager<T extends Datastore> implements DatastoreManager {

    private final Map<String, T> datastoreMap;

    public AbstractMapDatastoreManager(Map<String, T> datastoreMap) {
        this.datastoreMap = datastoreMap;
    }

    protected abstract T newDatastore(String key);
    
    @Override
    public synchronized T getDatastore(String key) {
        T instance = datastoreMap.get(key);
        
        if (instance == null) {
            instance = newDatastore(key);
            datastoreMap.put(key, instance);
        }
        
        return instance;
    }
}
