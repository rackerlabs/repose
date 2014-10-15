package org.openrepose.commons.utils.pooling;

public interface SimpleResourceContext<R> {

   void perform(R resource) throws ResourceContextException;
}
