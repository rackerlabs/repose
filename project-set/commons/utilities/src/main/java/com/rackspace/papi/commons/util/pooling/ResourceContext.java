package com.rackspace.papi.commons.util.pooling;

public interface ResourceContext<R, T> {

   T perform(R resource) throws ResourceContextException;
}
