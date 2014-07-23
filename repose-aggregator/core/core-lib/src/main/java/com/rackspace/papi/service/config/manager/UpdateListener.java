package com.rackspace.papi.service.config.manager;

public interface UpdateListener<T> {

    void configurationUpdated(T configurationObject);
    
    boolean isInitialized();
    
    
    
}
