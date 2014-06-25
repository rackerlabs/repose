package com.rackspace.papi.service;

public enum DefinedService {
    DISTRIBUTED_DATASTORE("dist-datastore");

    private final String serviceName;

    private DefinedService(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public static String listServices() {
        StringBuilder list = new StringBuilder();
        String prefix = "";

        for (DefinedService service : DefinedService.values()) {
            list.append(prefix);
            list.append(service.getServiceName());
            prefix = "\n";
        }

        return list.toString();
    }
}
