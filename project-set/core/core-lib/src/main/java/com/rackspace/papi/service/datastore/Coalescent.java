package com.rackspace.papi.service.datastore;

import java.io.Serializable;

/**
 * A coalescent object is a special type of serializable object that can be
 * coalesced with other coalescent objects of the same type with. This 
 * coalescing carries the expectation that the result of a coalescing the same 
 * coalescent objects will never change.
 * 
 * @author zinic
 */
public interface Coalescent<T> extends Serializable {

    Coalescent<T> coalesce(Coalescent<T>... targets);
}
