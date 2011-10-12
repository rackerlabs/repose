package com.rackspace.papi.filter.logic;

import com.rackspace.papi.commons.config.manager.UpdateListener;

/**
 *
 * @author Dan Daley
 */
public abstract class AbstractConfiguredFilterHandler<C> extends AbstractFilterLogicHandler implements UpdateListener<C> {
    private final LockableConfigurationListener<AbstractConfiguredFilterHandler<C>, C> listener;

    public AbstractConfiguredFilterHandler() {
       listener = new LockableConfigurationListener<AbstractConfiguredFilterHandler<C>, C>(this);
    }
    
    public UpdateListener<C> getConfigurationListener() {
       return listener.getConfigurationListener();
    }
    
    protected void lockConfigurationForRead() {
       listener.lockConfigurationForRead();
    }
    
    protected void unlockConfigurationForRead() {
       listener.unlockConfigurationForRead();
    }
    
    protected void lockConfigurationForUpdate() {
       listener.lockConfigurationForUpdate();
    }
    
    protected void unlockConfigurationForUpdate() {
       listener.unlockConfigurationForUpdate();
    }
    
   @Override
    public abstract void configurationUpdated(C configurationObject);
    
}
