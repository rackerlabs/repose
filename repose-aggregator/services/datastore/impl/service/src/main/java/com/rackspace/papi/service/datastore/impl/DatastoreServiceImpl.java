package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.service.datastore.*;
import com.rackspace.papi.service.datastore.impl.distributed.hash.HashRingDatastoreManager;
import com.rackspace.papi.service.datastore.impl.ehcache.EHCacheDatastoreManager;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component("datastoreService")
public class DatastoreServiceImpl implements DatastoreService {

   private final Map<String, DatastoreManager> localManagers;
   private final Map<String, DatastoreManager> distributedManagers;

   public DatastoreServiceImpl() {
      DatastoreManager manager = new EHCacheDatastoreManager();

      localManagers = new HashMap<String, DatastoreManager>();
      distributedManagers = new HashMap<String, DatastoreManager>();
      registerDatastoreManager(Datastore.DEFAULT_LOCAL, manager);
   }

   @Override
   public Datastore getDefaultDatastore() {
       final DatastoreManager defaultManager = localManagers.get(Datastore.DEFAULT_LOCAL);
       return defaultManager == null ? null : defaultManager.getDatastore();
   }

   @Override
   public Datastore getDatastore(String datastoreName) {
       if (localManagers.containsKey(datastoreName)) {
           return localManagers.get(datastoreName).getDatastore();
       }

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
       final DatastoreManager managerToUnregister;

       if (localManagers.containsKey(datastoreName)) {
           managerToUnregister = localManagers.get(datastoreName);
           localManagers.remove(datastoreName);
       } else {
           managerToUnregister = distributedManagers.get(datastoreName);
           distributedManagers.remove(datastoreName);
       }

       if (managerToUnregister != null) {
           managerToUnregister.destroy();
       }
   }

   @Override
   public void registerDatastoreManager(String datastoreManagerName, DatastoreManager manager) {
      final Map<String, DatastoreManager> registerTo = manager.isDistributed() ? distributedManagers : localManagers;
      registerTo.put(datastoreManagerName, new DatastoreManagerImpl(manager));
   }

   @Override
    public DistributedDatastore createDatastore(String datastoreName, DistDatastoreConfiguration configuration) {

        Datastore defaultDatastore = getDefaultDatastore();

        if (defaultDatastore == null) {
            final Collection<DatastoreManager> availableLocalDatstores = localManagers.values();

            if (!availableLocalDatstores.isEmpty()) {
                defaultDatastore = availableLocalDatstores.iterator().next().getDatastore();
            } else {
                throw new DatastoreServiceException("Unable to start Distributed Datastore Service");
            }
        }
        DatastoreManager manager = new HashRingDatastoreManager(configuration, defaultDatastore);
        registerDatastoreManager(datastoreName, manager);
        return (DistributedDatastore) manager.getDatastore();
    }
}
