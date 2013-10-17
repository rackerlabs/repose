package com.rackspace.papi.commons.config.manager;

import com.rackspace.papi.commons.util.thread.KeyedStackLock;

/**
 * User: joshualockwood
 * Date: 6/13/11
 * Time: 1:49 PM
 */
public abstract class LockedConfigurationUpdater<T> implements UpdateListener<T> {
    private final KeyedStackLock updateLock;
    private final Object updateKey;

    private boolean isInitialized = false;
   
    
    public LockedConfigurationUpdater(KeyedStackLock updateLock, Object updateKey) {
        this.updateLock = updateLock;
        this.updateKey = updateKey;
    }

    @Override
    public final void configurationUpdated(T configurationObject) {
        updateLock.lock(updateKey);

        try {
            onConfigurationUpdated(configurationObject);
        } finally {
            updateLock.unlock(updateKey);
        }
          isInitialized = true;
    }

    @Override
    public boolean isInitialized(){
     return isInitialized;
    }

  

    protected abstract void onConfigurationUpdated(T configurationObject);
}
