package com.rackspace.papi.service.datastore.distributed.impl.replicated;

public interface UpdateListener extends Runnable {

    void done();

    String getAddress();

    int getPort();
    
}
