package com.rackspace.papi.service.datastore.cluster;

import com.rackspace.papi.service.datastore.cluster.member.DroppedMember;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ThreadSafeClusterView implements MutableClusterView {

    private static final Comparator<InetSocketAddress> INET_SOCKET_ADDRESS_COMPARATOR = new Comparator<InetSocketAddress>() {

        @Override
        public int compare(InetSocketAddress o1, InetSocketAddress o2) {
            final BigInteger o1Address = new BigInteger(o1.getAddress().getAddress());
            final BigInteger o2Address = new BigInteger(o2.getAddress().getAddress());

            return o1Address.compareTo(o2Address);
        }
    };
    
    private static final int DEFAULT_REST_DURATION_IN_MILISECONDS = 25000;
    
    private final Set<DroppedMember> droppedMembers;
    private final List<InetSocketAddress> clusterMembers;
    private InetSocketAddress localAddress;

    public ThreadSafeClusterView() {
        clusterMembers = new LinkedList<InetSocketAddress>();
        droppedMembers = new HashSet<DroppedMember>();
    }

    private void normalizeClusterMembers() {
        // Normalize the member order
        Collections.sort(clusterMembers, INET_SOCKET_ADDRESS_COMPARATOR);
    }

    private void updateDroppedMembers() {
        final Iterator<DroppedMember> droppedMemberIterator = droppedMembers.iterator();
        final long now = System.currentTimeMillis();

        while (droppedMemberIterator.hasNext()) {
            final DroppedMember droppedMember = droppedMemberIterator.next();

            if (droppedMember.shouldRetry(now)) {
                droppedMemberIterator.remove();
                clusterMembers.add(droppedMember.getMemberAddress());
            }
        }

        normalizeClusterMembers();
    }

    private boolean hasDropopedMember(InetSocketAddress address) {
        for (DroppedMember member : droppedMembers) {
            if (member.getMemberAddress().equals(address)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public synchronized void memberDropoped(InetSocketAddress address) {
        if (!clusterMembers.remove(address)) {
            if (!hasDropopedMember(address)) {
                throw new IllegalArgumentException("Member " + address.toString() + " is not part of this cluster view.");
            }
        } else {
            droppedMembers.add(new DroppedMember(address, DEFAULT_REST_DURATION_IN_MILISECONDS));
        }
    }

    @Override
    public synchronized void updateMembers(InetSocketAddress[] view) {
        droppedMembers.clear();
        clusterMembers.clear();

        clusterMembers.addAll(Arrays.asList(view));

        normalizeClusterMembers();
    }

    @Override
    public synchronized void updateLocal(InetSocketAddress local) {
        localAddress = local;
    }

    @Override
    public synchronized InetSocketAddress[] members() {
        updateDroppedMembers();

        return clusterMembers.toArray(new InetSocketAddress[clusterMembers.size()]);
    }

    @Override
    public synchronized InetSocketAddress localMember() {
        return localAddress;
    }
}
