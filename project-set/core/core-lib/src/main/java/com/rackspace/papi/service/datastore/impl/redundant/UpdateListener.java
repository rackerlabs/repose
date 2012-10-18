package com.rackspace.papi.service.datastore.impl.redundant;

public interface UpdateListener extends Runnable {

    void done();

    String getAddress();

    int getPort();
    
}
