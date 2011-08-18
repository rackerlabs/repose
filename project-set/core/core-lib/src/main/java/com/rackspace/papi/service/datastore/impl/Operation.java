package com.rackspace.papi.service.datastore.impl;

import java.util.concurrent.TimeUnit;

public interface Operation {

    public enum Type {

        PUT, GET, DELETE
    }

    OperationFuture getFuture();

    String getKey();

    TimeUnit getTimeUnit();

    int getTtl();

    Type getType();

    byte[] getValue();

    boolean hasTtlInformation();
}
