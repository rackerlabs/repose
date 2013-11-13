package com.rackspace.papi.service.datastore;

import java.util.Collection;

public interface DatastoreService {

   String DEFAULT_LOCAL = "local/default";

   /**
    * Always returns the default, local (read L1) cache that has been registered
    * with the datastore service.
    */
   DatastoreManager defaultDatastore();

   DatastoreManager getDatastore(String datastoreName);

   Collection<DatastoreManager> availableLocalDatastores();

   Collection<DatastoreManager> availableDistributedDatastores();

   // TODO:Enhancement - Use a future object here to communicate committal of the datastore being unregistered
   void unregisterDatastoreManager(String datastoreManagerName);

   void registerDatastoreManager(String datastoreManagerName, DatastoreManager manager);
}
