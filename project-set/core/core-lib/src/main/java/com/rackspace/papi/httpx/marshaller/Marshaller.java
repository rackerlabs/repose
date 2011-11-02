package com.rackspace.papi.httpx.marshaller;

import java.io.InputStream;

/**
 * @author fran
 */
public interface Marshaller<T> {
    public InputStream marshall(T type);
}
