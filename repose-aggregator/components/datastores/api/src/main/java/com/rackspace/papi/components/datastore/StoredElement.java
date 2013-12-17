package com.rackspace.papi.components.datastore;

/**
 * A Stored Element, consisting of a key, byte array of a value.
 */
public interface StoredElement {

    /**
     * Return the key associated with this stored element
     * @return
     */
    String elementKey();

    /**
     * Return a byte array of the stored element
     * @return
     */
    byte[] elementBytes();

    /**
     * Allows for a StoredElement to represent an unstored element (null byte array)
     * @return
     */
    boolean elementIsNull();

    /**
     * Class representing the stored element
     * @return
     */
    Class<?> elementClass();

    /**
     * Given a class, returns true is the element can be serialized into the Class, false if not.
     * @param clazz
     * @return
     */
    boolean elementIs(Class clazz);
    
     <T> T elementAs(Class<T> clazz);
}
