package org.openrepose.commons.config.manager;

public interface UpdateListener<T> {

    void configurationUpdated(T configurationObject) throws UpdateFailedException;
    
    boolean isInitialized();
    
    
    
}
