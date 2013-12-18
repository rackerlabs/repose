package com.rackspace.papi.components.datastore.impl.replicated;

public interface SubscriptionListener {

    void join(String host, int port);

    void unsubscribe();
    
}
