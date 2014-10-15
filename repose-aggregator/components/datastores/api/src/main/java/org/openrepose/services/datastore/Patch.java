package org.openrepose.services.datastore;

/**
 * An interface for a Patch (of T).
 *
 * T - this is the abstraction of the data that gets stored in the datastore
 */
public interface Patch<T> {
    T newFromPatch();
}
