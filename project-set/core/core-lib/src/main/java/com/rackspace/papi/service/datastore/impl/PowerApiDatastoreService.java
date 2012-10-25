package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("datastoreService")
public class PowerApiDatastoreService implements DatastoreService {

   private final Map<String, DatastoreManager> localManagers;
   private final Map<String, DatastoreManager> distributedManagers;

   public PowerApiDatastoreService() {
      localManagers = new HashMap<String, DatastoreManager>();
      distributedManagers = new HashMap<String, DatastoreManager>();
   }

   @Override
   public DatastoreManager defaultDatastore() {
      return localManagers.get(DatastoreService.DEFAULT_LOCAL);
   }

   @Override
   public DatastoreManager getDatastore(String datastoreName) {
      final DatastoreManager localManager = getLocalDatastore(datastoreName);

      return localManager == null ? getDistributedDatastore(datastoreName) : localManager;
   }

   private DatastoreManager availableOrNull(DatastoreManager manager) {
      return manager != null && manager.isAvailable() ? manager : null;
   }

   private DatastoreManager getLocalDatastore(String datastoreName) {
      return availableOrNull(localManagers.get(datastoreName));
   }

   private DatastoreManager getDistributedDatastore(String datastoreName) {
      return availableOrNull(distributedManagers.get(datastoreName));
   }

   @Override
   public Collection<DatastoreManager> availableLocalDatastores() {
      return filterAvailableDatastoreManagers(localManagers.values());
   }

   @Override
   public Collection<DatastoreManager> availableDistributedDatastores() {
      return filterAvailableDatastoreManagers(distributedManagers.values());
   }

   private Collection<DatastoreManager> filterAvailableDatastoreManagers(Collection<DatastoreManager> managers) {
      final Set<DatastoreManager> availableDistributedDatastores = new HashSet<DatastoreManager>();

      for (DatastoreManager manager : managers) {
         if (manager.isAvailable()) {
            availableDistributedDatastores.add(manager);
         }
      }

      return availableDistributedDatastores;
   }

   @Override
   public void unregisterDatastoreManager(String datastoreManagerName) {
      if (localManagers.remove(datastoreManagerName) == null) {
         distributedManagers.remove(datastoreManagerName);
      }
   }

   @Override
   public void registerDatastoreManager(String datastoreManagerName, DatastoreManager manager) {
      final Map<String, DatastoreManager> registerTo = manager.isDistributed() ? distributedManagers : localManagers;

      registerTo.put(datastoreManagerName, new AvailablityGuard(manager, this));
   }
}
