package com.rackspace.papi.service.datastore.cluster;

import java.net.InetSocketAddress;

public interface MutableClusterView extends ClusterView {

    void memberDropoped(InetSocketAddress address);

    void updateMembers(InetSocketAddress[] newMembers);

    MutableClusterView copy();
}
