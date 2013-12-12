package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import org.springframework.stereotype.Component;

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
      if (localManagers.remove(datastoreManagerName) == null) {
         distributedManagers.remove(datastoreManagerName);
      }
   }

   @Override
   public void registerDatastoreManager(String datastoreManagerName, DatastoreManager manager) {
      final Map<String, DatastoreManager> registerTo = manager.isDistributed() ? distributedManagers : localManagers;
      registerTo.put(datastoreManagerName, new DatastoreManagerImpl(manager));
   }
}
