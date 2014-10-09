package org.openrepose.commons.utils.pooling;

/**
 *
 * 
 */
public interface ConstructionStrategy<T> {

    T construct() throws ResourceConstructionException;
}
