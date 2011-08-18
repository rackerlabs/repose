package com.rackspace.papi.service.datastore;

public interface StoredElement {

    String elementKey();

    byte[] elementBytes();
    
    boolean elementIsNull();

    Class<?> elementClass();
    
    boolean elementIs(Class clazz);
    
     <T> T elementAs(Class<T> clazz);
}
