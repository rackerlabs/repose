package com.rackspace.papi.service.datastore.cluster;

import java.net.InetSocketAddress;

public interface MutableClusterView extends ClusterView {

    void updateMembers(InetSocketAddress[] newMembers);
    
    void updateLocal(InetSocketAddress local);
}
