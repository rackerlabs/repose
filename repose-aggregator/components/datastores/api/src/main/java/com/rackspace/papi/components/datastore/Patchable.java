package com.rackspace.papi.components.datastore;

/**
 * An interface for a Patchable (of T, P).
 *
 * T - type variable of element
 * P - type variable of patchable object
 *
 */
public interface Patchable<T, P extends Patch<T>> {
    public T applyPatch(P in);
}
