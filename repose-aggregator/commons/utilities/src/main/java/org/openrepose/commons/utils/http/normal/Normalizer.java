package org.openrepose.commons.utils.http.normal;

public interface Normalizer<T> {

    T normalize(T source);
}
