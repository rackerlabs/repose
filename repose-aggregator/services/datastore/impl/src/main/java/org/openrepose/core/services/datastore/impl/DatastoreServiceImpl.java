package org.openrepose.core.services.datastore.impl;

import org.openrepose.core.services.datastore.impl.distributed.HashRingDatastoreManager;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.datastore.DatastoreManager;
import org.openrepose.core.services.datastore.DatastoreService;
import org.openrepose.core.services.datastore.distributed.ClusterConfiguration;
import org.openrepose.core.services.datastore.distributed.DistributedDatastore;
import org.openrepose.core.services.datastore.impl.ehcache.EHCacheDatastoreManager;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

@Named
public class DatastoreServiceImpl implements DatastoreService {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DatastoreServiceImpl.class);

    private final DatastoreManager localDatastoreManager;
    private final Map<String, DatastoreManager> distributedManagers;

    public DatastoreServiceImpl() {
        localDatastoreManager = new EHCacheDatastoreManager();
        distributedManagers = new HashMap<>();
    }

    @PreDestroy
    public void destroy() {
        LOG.info("Destroying datastore service context");
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
                LOG.warn("Failed to shutdown datastore " + datastoreName + " with exception " + e.getMessage(), e);
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
