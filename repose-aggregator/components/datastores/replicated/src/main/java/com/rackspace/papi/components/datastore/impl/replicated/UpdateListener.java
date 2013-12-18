package com.rackspace.papi.components.datastore.impl.replicated;

public interface UpdateListener extends Runnable {

    void done();

    String getAddress();

    int getPort();
    
}
