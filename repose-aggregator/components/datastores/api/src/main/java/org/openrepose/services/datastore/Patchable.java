package org.openrepose.services.datastore;

/**
 * An interface for a Patchable (of T, P).
 *
 * T - type variable of element
 * P - type variable of patchable object
 *
 */
public interface Patchable<T, P extends Patch<T>> {
    T applyPatch(P in);
}
