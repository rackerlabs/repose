package com.rackspace.papi.commons.util.pooling;

public interface Pool<R> {

    int DEFAULT_MAX_POOL_SIZE = 5, DEFAULT_MIN_POOL_SIZE = 1;

    int size();

    <T> T use(ResourceContext<R, T> newContext);

    void use(SimpleResourceContext<R> newContext);

    void setMaximumPoolSize(int newSize);

    void setMinimumPoolSize(int newSize);
}
