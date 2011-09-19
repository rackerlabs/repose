package com.rackspace.papi.service.datastore.impl;

import java.util.concurrent.TimeUnit;

public interface Operation {

    public enum Type {

        PUT, PUT_BY_HASH, GET, GET_BY_HASH, DELETE
    }

    OperationFuture getFuture();

    String getKey();

    TimeUnit getTimeUnit();

    int getTtl();

    Type getType();

    byte[] getValue();

    boolean hasTtlInformation();
}
