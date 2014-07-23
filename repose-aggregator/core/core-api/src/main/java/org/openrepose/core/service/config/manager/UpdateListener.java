package org.openrepose.core.service.config.manager;

public interface UpdateListener<T> {

    void configurationUpdated(T configurationObject);
    
    boolean isInitialized();
    
    
    
}
