package com.rackspace.papi.service;

public enum Service {
    DISTRIBUTED_DATASTORE("dist-datastore");

    private final String serviceName;

    private Service(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
