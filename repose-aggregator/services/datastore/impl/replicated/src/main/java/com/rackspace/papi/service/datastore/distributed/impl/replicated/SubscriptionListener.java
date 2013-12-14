package com.rackspace.papi.service.datastore.distributed.impl.replicated;

public interface SubscriptionListener {

    void join(String host, int port);

    void unsubscribe();
    
}
