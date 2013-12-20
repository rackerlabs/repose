package com.rackspace.papi.commons.util.pooling;

public interface SimpleResourceContext<R> {

   void perform(R resource) throws ResourceContextException;
}
