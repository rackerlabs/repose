package com.rackspace.papi.service.datastore;

public interface DatastoreManager {

   Datastore getDatastore() throws DatastoreUnavailableException;

   boolean isAvailable();

   boolean isDistributed();
   
   String getName();


   void destroy();
}
