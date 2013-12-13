package com.rackspace.papi.service.datastore;

public class LocalDatastoreConfiguration implements DatastoreConfiguration {

    private String name;

    public LocalDatastoreConfiguration(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
