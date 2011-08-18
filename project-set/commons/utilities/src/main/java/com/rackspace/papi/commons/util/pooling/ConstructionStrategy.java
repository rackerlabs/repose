package com.rackspace.papi.commons.util.pooling;

/**
 *
 * 
 */
public interface ConstructionStrategy<T> {

    T construct() throws ResourceConstructionException;
}
