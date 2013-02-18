package com.rackspace.papi.commons.config.manager;

import com.rackspace.papi.commons.config.resource.ConfigurationResource;

public interface UpdateListener<T> {

    void configurationUpdated(T configurationObject);
    
    boolean isInitialized();
    
    
    
}
