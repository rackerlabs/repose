package com.rackspace.papi.commons.config.manager;

public interface UpdateListener<T> {

    void configurationUpdated(T configurationObject);
    
    boolean isInitialized();
    
   
}
