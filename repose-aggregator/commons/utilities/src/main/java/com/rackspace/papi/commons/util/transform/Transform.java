package com.rackspace.papi.commons.util.transform;

public interface Transform<S, T> {

   T transform(S source);
}
