package com.rackspace.papi.service.datastore.cluster;

import com.rackspace.papi.service.datastore.cluster.member.ClusterMember;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class ThreadSafeClusterView implements MutableClusterView {

    private static final Comparator<ClusterMember> CLUSTER_MEMBER_COMPARATOR = new Comparator<ClusterMember>() {

        @Override
        public int compare(ClusterMember o1, ClusterMember o2) {
            final BigInteger o1Address = new BigInteger(o1.getMemberAddress().getAddress().getAddress());
            final BigInteger o2Address = new BigInteger(o2.getMemberAddress().getAddress().getAddress());

            return o1Address.compareTo(o2Address);
        }
    };
    
    private static final int DEFAULT_REST_DURATION_IN_MILISECONDS = 25000;
    private final List<ClusterMember> clusterMembers;
    private InetSocketAddress localAddress;

    public ThreadSafeClusterView() {
        clusterMembers = new LinkedList<ClusterMember>();
    }

    private static void normalizeClusterMembers(List<ClusterMember> members) {
        // Normalize the member order
        Collections.sort(members, CLUSTER_MEMBER_COMPARATOR);
    }

    @Override
    public synchronized void memberDropoped(InetSocketAddress address) {
        for (ClusterMember member : clusterMembers) {
            if (member.getMemberAddress().equals(address)) {
                member.setOffline();
                break;
            }
        }
    }

    @Override
    public synchronized void updateMembers(InetSocketAddress[] view) {
        clusterMembers.clear();

        for (InetSocketAddress memberAddress : view) {
            clusterMembers.add(new ClusterMember(memberAddress, DEFAULT_REST_DURATION_IN_MILISECONDS));
        }

        normalizeClusterMembers(clusterMembers);
    }

    @Override
    public synchronized void updateLocal(InetSocketAddress local) {
        localAddress = local;
    }

    @Override
    public synchronized InetSocketAddress[] members() {
        final LinkedList<InetSocketAddress> activeClusterMembers = new LinkedList<InetSocketAddress>();

        for (ClusterMember member : clusterMembers) {
            if (member.isOnline() || member.shouldRetry()) {
                activeClusterMembers.add(member.getMemberAddress());
            }
        }

        return activeClusterMembers.toArray(new InetSocketAddress[activeClusterMembers.size()]);
    }

    @Override
    public synchronized InetSocketAddress localMember() {
        return localAddress;
    }
}
