package com.rackspace.papi.service.datastore.impl.replicated;

public interface UpdateListener extends Runnable {

    void done();

    String getAddress();

    int getPort();
    
}
