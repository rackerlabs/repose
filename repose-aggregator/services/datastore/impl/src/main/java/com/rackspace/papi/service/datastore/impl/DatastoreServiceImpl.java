package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.components.datastore.Datastore;
import com.rackspace.papi.components.datastore.DatastoreManager;
import com.rackspace.papi.components.datastore.distributed.ClusterConfiguration;
import com.rackspace.papi.components.datastore.distributed.DistributedDatastore;
import com.rackspace.papi.components.datastore.impl.distributed.HashRingDatastoreManager;
import com.rackspace.papi.components.datastore.impl.ehcache.EHCacheDatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;

@Component
public class DatastoreServiceImpl implements DatastoreService {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreServiceImpl.class);

    private final DatastoreManager localDatastoreManager;
    private final Map<String, DatastoreManager> distributedManagers;

    @Autowired
    public DatastoreServiceImpl() {
        localDatastoreManager = new EHCacheDatastoreManager();
        distributedManagers = new HashMap<String, DatastoreManager>();
    }

    @PreDestroy
    public void destroy() {
        shutdown();
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
                LOG.warn("Failed to shutdown datastore "+ datastoreName +" with exception " + e.getMessage(), e);
            }
        }
    }

    @Override
    public DistributedDatastore createDatastore(String datastoreName, ClusterConfiguration configuration) {
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
