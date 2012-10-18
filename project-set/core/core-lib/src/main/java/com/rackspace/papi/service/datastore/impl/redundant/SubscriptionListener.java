package com.rackspace.papi.service.datastore.impl.redundant;

public interface SubscriptionListener {

    void join(String host, int port);

    void unsubscribe();
    
}
