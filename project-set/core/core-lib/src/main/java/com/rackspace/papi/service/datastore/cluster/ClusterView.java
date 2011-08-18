package com.rackspace.papi.service.datastore.cluster;

import java.net.InetSocketAddress;

public interface ClusterView {

    InetSocketAddress[] members();
    
    InetSocketAddress local();
}
