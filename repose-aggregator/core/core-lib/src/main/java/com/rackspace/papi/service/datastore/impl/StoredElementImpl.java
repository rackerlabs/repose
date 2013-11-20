package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.commons.util.ArrayUtilities;
import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.StoredElement;

import java.io.Serializable;

public class StoredElementImpl implements StoredElement {

    private final String key;
    private final byte[] elementBytes;
    private Serializable resolvedElement;

    public StoredElementImpl(String key, byte[] elementBytes) {
        this.elementBytes = ArrayUtilities.nullSafeCopy(elementBytes);
        this.key = key;
    }

    private synchronized Serializable getResolvedElement() {
        if (resolvedElement == null && !elementIsNull()) {
            try {
                resolvedElement = ObjectSerializer.instance().readObject(elementBytes);
            } catch (Exception ex) {
                throw new DatastoreServiceException("Unable to marshall a java object from stored element contents. Reason: " + ex.getMessage(), ex);
            }
        }

        return resolvedElement;
    }

    @Override
    public byte[] elementBytes() {
        return elementBytes;
    }

    @Override
    public Class<?> elementClass() {
        return !elementIsNull() ? getResolvedElement().getClass() : null;
    }

    @Override
    public String elementKey() {
        return key;
    }

    @Override
    public boolean elementIsNull() {
        return elementBytes == null || elementBytes.length == 0;
    }

    @Override
    public boolean elementIs(Class clazz) {
        return !elementIsNull() && clazz.isAssignableFrom(getResolvedElement().getClass());
    }

    @Override
    public <T> T elementAs(Class<T> clazz) {
        if (elementIsNull()) {
            return null;
        }
        
        if (clazz.isAssignableFrom(getResolvedElement().getClass())) {
            return clazz.cast(getResolvedElement());
        }

        throw new ClassCastException("Attempted to cast cached element with class '"
                + getResolvedElement().getClass().getName() + "' to class '" + clazz.getName());
    }
}
