package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.DatastoreUnavailableException;

/**
 *
 * @author zinic
 */
public final class AvailablityGuard implements DatastoreManager {

   private final DatastoreManager manager;
   private final DatastoreService service;

   public AvailablityGuard(DatastoreManager manager, DatastoreService service) {
      this.manager = manager;
      this.service = service;
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
         throw new DatastoreUnavailableException(service, "Datastore  " + manager.toString() + " is currently unavailable.");
      }

      return manager.getDatastore();
   }
}
