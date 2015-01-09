package org.openrepose.core.services.datastore.distributed;

import org.openrepose.core.services.datastore.DatastoreOperationException;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public interface NotifiableDatastore {

    boolean remove(String key, boolean notify) throws DatastoreOperationException;
    void put(String key, Serializable value, boolean notify) throws DatastoreOperationException;
    void put(String key, Serializable value, int ttl, TimeUnit timeUnit, boolean notify) throws DatastoreOperationException;
}
