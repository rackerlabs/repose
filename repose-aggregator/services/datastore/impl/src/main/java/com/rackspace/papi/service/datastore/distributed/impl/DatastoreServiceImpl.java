package com.rackspace.papi.service.datastore.distributed.impl;

import com.rackspace.papi.components.datastore.Datastore;
import com.rackspace.papi.components.datastore.DatastoreManager;
import com.rackspace.papi.components.datastore.distributed.DistDatastoreConfiguration;
import com.rackspace.papi.components.datastore.distributed.DistributedDatastore;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.distributed.impl.ehcache.EHCacheDatastoreManager;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("datastoreService")
public class DatastoreServiceImpl implements DatastoreService {

    private final DatastoreManager localDatastoreManager;
    private final Map<String, DatastoreManager> distributedManagers;

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DatastoreServiceImpl.class);

    public DatastoreServiceImpl() {
        localDatastoreManager = new EHCacheDatastoreManager();
        distributedManagers = new HashMap<String, DatastoreManager>();
    }

    @Override
    public Datastore getDefaultDatastore() {
        return localDatastoreManager.getDatastore();
    }

    @Override
    public Datastore getDatastore(String datastoreName) {
        if (distributedManagers.containsKey(datastoreName)) {
            return distributedManagers.get(datastoreName).getDatastore();
        }

        return null;
    }

    @Override
    public DistributedDatastore getDistributedDatastore() {
        if (!distributedManagers.isEmpty()) {
            return (DistributedDatastore) distributedManagers.values().iterator().next().getDatastore();
        }
        return null;
    }

    @Override
    public void destroyDatastore(String datastoreName) {
        final DatastoreManager managerToUnregister = distributedManagers.get(datastoreName);

        if (managerToUnregister != null) {
            distributedManagers.remove(datastoreName);
            try {
                managerToUnregister.destroy();
            } catch (Exception e) {
                LOG.warn("Failed to shutdown datastore {} with exception {}", datastoreName, e.getMessage());
            }
        }
    }

    @Override
    public DistributedDatastore createDatastore(String datastoreName, DistDatastoreConfiguration configuration) {
        DatastoreManager manager = new HashRingDatastoreManager(configuration, localDatastoreManager.getDatastore());
        distributedManagers.put(datastoreName, manager);
        return (DistributedDatastore) manager.getDatastore();
    }

    @Override
    public void shutdown() {
        for (String datastoreName : distributedManagers.keySet()) {
            destroyDatastore(datastoreName);
        }
        localDatastoreManager.destroy();
    }
}
