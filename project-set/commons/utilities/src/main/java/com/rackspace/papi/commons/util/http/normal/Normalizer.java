package com.rackspace.papi.commons.util.http.normal;

public interface Normalizer<T> {

    T normalize(T source);
}
