package com.rackspace.papi.components.datastore;

/**
 * A manager for {@link Datastore}s that manages their creation and lifecycle.
 */
public interface DatastoreManager {

    /**
     * Get the datastore that is being managed.
     * @return
     * @throws DatastoreUnavailableException if no datastore is available
     */
    Datastore getDatastore() throws DatastoreUnavailableException;

    /**
     * Returns true if the datastore being managed is distributed, false if the datastore support local storage only.
     * @return
     */
    boolean isDistributed();

    /**
     * Perform any shutdown needed on the datastore being managed.
     */
    void destroy();
}
