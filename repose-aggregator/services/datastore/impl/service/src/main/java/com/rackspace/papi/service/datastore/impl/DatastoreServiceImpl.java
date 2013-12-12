package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.service.datastore.*;
import com.rackspace.papi.service.datastore.impl.distributed.hash.HashRingDatastoreManager;
import com.rackspace.papi.service.datastore.impl.ehcache.EHCacheDatastoreManager;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import java.util.*;

@Component("datastoreService")
public class DatastoreServiceImpl implements DatastoreService {

   private final Map<String, DatastoreManager> localManagers;
   private final Map<String, DatastoreManager> distributedManagers;

   public DatastoreServiceImpl() {
      localManagers = new HashMap<String, DatastoreManager>();
      distributedManagers = new HashMap<String, DatastoreManager>();
   }

   @Override
   public DatastoreManager defaultDatastore() {
      return localManagers.get(Datastore.DEFAULT_LOCAL);
   }

   @Override
   public DatastoreManager getDatastore(String datastoreName) {
      final DatastoreManager localManager = getLocalDatastore(datastoreName);

      return localManager == null ? getDistributedDatastore(datastoreName) : localManager;
   }

   private DatastoreManager getLocalDatastore(String datastoreName) {
      return localManagers.get(datastoreName);
   }

   private DatastoreManager getDistributedDatastore(String datastoreName) {
      return distributedManagers.get(datastoreName);
   }

   @Override
   public Collection<DatastoreManager> availableLocalDatastores() {
      return localManagers.values();
   }

   @Override
   public Collection<DatastoreManager> availableDistributedDatastores() {
      return distributedManagers.values();
   }

   @Override
   public void unregisterDatastoreManager(String datastoreManagerName) {
       final DatastoreManager managerToUnregister;

       if (localManagers.containsKey(datastoreManagerName)) {
           managerToUnregister = localManagers.get(datastoreManagerName);
           localManagers.remove(datastoreManagerName);
       } else {
           managerToUnregister = distributedManagers.get(datastoreManagerName);
           distributedManagers.remove(datastoreManagerName);
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
    public void registerDatastoreManager(String datastoreManagerName, DatastoreConfiguration configuration) {
        if (datastoreManagerName.equals(Datastore.DEFAULT_LOCAL)) {
            DatastoreManager manager = new EHCacheDatastoreManager((LocalDatastoreConfiguration) configuration);
            registerDatastoreManager(Datastore.DEFAULT_LOCAL, manager);
        } else {

            DatastoreManager localDatastoreManager = defaultDatastore();

            if (localDatastoreManager == null) {
                final Collection<DatastoreManager> availableLocalDatstores = availableLocalDatastores();

                if (!availableLocalDatstores.isEmpty()) {
                    localDatastoreManager = availableLocalDatstores.iterator().next();
                } else {
                    throw new DatastoreServiceException("Unable to start Distributed Datastore Service");
                }
            }
            DatastoreManager manager = new HashRingDatastoreManager((DistDatastoreConfiguration) configuration, localDatastoreManager.getDatastore());
            registerDatastoreManager(datastoreManagerName, manager);
        }
    }
}
