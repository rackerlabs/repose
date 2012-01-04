package com.rackspace.papi.service.datastore;

import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.common.DatastoreUnavailableException;

public interface DatastoreManager extends Destroyable {

   Datastore getDatastore() throws DatastoreUnavailableException;

   boolean isAvailable();

   boolean isDistributed();
}
