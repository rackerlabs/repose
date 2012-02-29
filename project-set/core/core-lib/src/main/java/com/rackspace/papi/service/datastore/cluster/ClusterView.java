package com.rackspace.papi.service.datastore.cluster;

import java.net.InetSocketAddress;
import java.net.SocketException;

public interface ClusterView {

    InetSocketAddress[] members();

    boolean isLocal(InetSocketAddress addr) throws SocketException;
}
