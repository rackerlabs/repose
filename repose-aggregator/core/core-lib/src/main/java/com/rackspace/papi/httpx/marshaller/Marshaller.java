package com.rackspace.papi.httpx.marshaller;

import java.io.InputStream;

/**
 * @author fran
 */
public interface Marshaller<T> {
    InputStream marshall(T type);
}
