package com.rackspace.papi.service.datastore;

import com.rackspace.papi.commons.util.Destroyable;

public interface DatastoreManager extends Destroyable {

   Datastore getDatastore();
   
   boolean isAvailable();

   boolean isDistributed();
}
