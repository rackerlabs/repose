package com.rackspace.papi.components.datastore;

/**
 * An interface for a Patch (of TStoredElement).
 *
 * TStoredElement - this is the abstraction of the data that gets stored in the datastore
 */
public interface Patch<TStoredElement> {
    public TStoredElement newFromPatch();
}
