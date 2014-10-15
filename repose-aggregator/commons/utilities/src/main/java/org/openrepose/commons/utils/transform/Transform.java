package org.openrepose.commons.utils.transform;

public interface Transform<S, T> {

   T transform(S source);
}
