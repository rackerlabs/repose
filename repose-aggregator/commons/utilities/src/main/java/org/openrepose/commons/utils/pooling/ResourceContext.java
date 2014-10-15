package org.openrepose.commons.utils.pooling;

public interface ResourceContext<R, T> {

   T perform(R resource) throws ResourceContextException;
}
