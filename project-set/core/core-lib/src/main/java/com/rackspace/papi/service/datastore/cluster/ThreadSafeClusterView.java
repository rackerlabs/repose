package com.rackspace.papi.service.datastore.cluster;

import com.rackspace.papi.commons.util.net.NetworkInterfaceProvider;
import com.rackspace.papi.commons.util.net.StaticNetworkInterfaceProvider;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.service.datastore.cluster.member.ClusterMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class ThreadSafeClusterView implements MutableClusterView {

   private static final Logger LOG = LoggerFactory.getLogger(ThreadSafeClusterView.class);
   private static final Comparator<ClusterMember> CLUSTER_MEMBER_COMPARATOR = new Comparator<ClusterMember>() {

      @Override
      public int compare(ClusterMember o1, ClusterMember o2) {
         final BigInteger o1Address = new BigInteger(o1.getMemberAddress().getAddress().getAddress());
         final BigInteger o2Address = new BigInteger(o2.getMemberAddress().getAddress().getAddress());

         return o1Address.compareTo(o2Address);
      }
   };
   private static final int DEFAULT_REST_DURATION_IN_MILISECONDS = 10000;
   private final NetworkInterfaceProvider networkInterfaceProvider;
   private final List<ClusterMember> clusterMembers;
   private final ServicePorts listenPorts;

   public ThreadSafeClusterView(ServicePorts listenPorts) {
      this(new LinkedList<ClusterMember>(), listenPorts);
   }

   public ThreadSafeClusterView(List<ClusterMember> clusterMembers, ServicePorts listenPorts) {
      this(StaticNetworkInterfaceProvider.getInstance(), new LinkedList<ClusterMember>(clusterMembers), listenPorts);
   }

   public ThreadSafeClusterView(NetworkInterfaceProvider networkInterfaceProvider, List<ClusterMember> clusterMembers, ServicePorts listenPorts) {
      this.networkInterfaceProvider = networkInterfaceProvider;
      this.clusterMembers = clusterMembers;
      this.listenPorts = listenPorts;
   }

   private static void normalizeClusterMembers(List<ClusterMember> members) {
      // Normalize the member order
      Collections.sort(members, CLUSTER_MEMBER_COMPARATOR);
   }

   @Override
   public ThreadSafeClusterView copy() {
      return new ThreadSafeClusterView(clusterMembers, listenPorts);
   }

   @Override
   public synchronized void memberDamaged(InetSocketAddress address, String reason) {
      for (ClusterMember member : clusterMembers) {
         if (member.getMemberAddress().equals(address)) {
            LOG.warn("Cluster member \"" + member.getMemberAddress().toString()
                    + "\" has been marked as damaged. We will retry this cluster "
                    + "member later. Reason: " + reason);

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
   public synchronized InetSocketAddress[] members() {
      final LinkedList<InetSocketAddress> activeClusterMembers = new LinkedList<InetSocketAddress>();

      for (ClusterMember member : clusterMembers) {
         final boolean memberIsOnline = member.isOnline();

         if (memberIsOnline || member.shouldRetry()) {
            if (!memberIsOnline) {
               LOG.warn("Cluster member \"" + member.getMemberAddress().toString() + "\" was previously marked as damaged but is now eligible for retry.");
            }

            activeClusterMembers.add(member.getMemberAddress());
         }
      }

      return activeClusterMembers.toArray(new InetSocketAddress[activeClusterMembers.size()]);
   }

   @Override
   public synchronized boolean hasDamagedMembers() {
      for (ClusterMember member : clusterMembers) {
         if (!member.isOnline()) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean isLocal(InetSocketAddress addr) throws SocketException {
      boolean havePort = false;
      for (Port port : listenPorts) {
         if (addr.getPort() == port.getPort()) {
            havePort = true;
            break;
         }
      }

      if (havePort && networkInterfaceProvider.hasInterfaceFor(addr.getAddress())) {
         return true;
      }

      return false;
   }

   @Override
   public ServicePorts getListenPorts() {
      return listenPorts;
   }
}
