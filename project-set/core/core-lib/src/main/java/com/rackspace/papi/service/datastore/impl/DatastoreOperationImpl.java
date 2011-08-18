package com.rackspace.papi.service.datastore.impl;

import java.util.concurrent.TimeUnit;

public class DatastoreOperationImpl implements Operation {

    private final OperationFuture getFuture;
    private final Type type;
    private final String key;
    private final byte[] value;
    private final int ttl;
    private final TimeUnit timeUnit;

    public DatastoreOperationImpl(Type type, String key) {
        this(type, key, null, -1, null);
    }

    public DatastoreOperationImpl(Type type, String key, byte[] value) {
        this(type, key, value, -1, null);
    }

    public DatastoreOperationImpl(Type type, String key, byte[] value, int ttl, TimeUnit timeUnit) {
        this.type = type;
        this.key = key;
        this.value = value;
        this.ttl = ttl;
        this.timeUnit = timeUnit;

        getFuture = new OperationFuture();
    }

    @Override
    public OperationFuture getFuture() {
        return getFuture;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public int getTtl() {
        return ttl;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public byte[] getValue() {
        return value;
    }

    @Override
    public boolean hasTtlInformation() {
        return ttl > 0 && timeUnit != null;
    }
}
