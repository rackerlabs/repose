package com.rackspace.papi.service.datastore.cluster;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Comparator;

public class ThreadSafeClusterView implements MutableClusterView {

    private InetSocketAddress localAddress;
    private InetSocketAddress[] clusterMembers;

    @Override
    public synchronized void updateMembers(InetSocketAddress[] view) {
        clusterMembers = view;
        
        // Normalize the host array
        Arrays.sort(clusterMembers, new Comparator<InetSocketAddress>() {

            @Override
            public int compare(InetSocketAddress o1, InetSocketAddress o2) {
                final BigInteger o1Address = new BigInteger(o1.getAddress().getAddress());
                final BigInteger o2Address = new BigInteger(o2.getAddress().getAddress());

                return o1Address.compareTo(o2Address);
            }
        });
    }

    @Override
    public synchronized void updateLocal(InetSocketAddress local) {
        localAddress = local;
    }
    
    @Override
    public synchronized InetSocketAddress[] members() {
        return Arrays.copyOf(clusterMembers, clusterMembers.length);
    }

    @Override
    public synchronized InetSocketAddress local() {
        return localAddress;
    }
}
