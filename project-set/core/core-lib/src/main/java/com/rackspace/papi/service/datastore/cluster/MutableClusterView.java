package com.rackspace.papi.service.datastore.cluster;

import com.rackspace.papi.domain.ServicePorts;

import java.net.InetSocketAddress;

public interface MutableClusterView extends ClusterView {

   void memberDamaged(InetSocketAddress address, String reason);

   void updateMembers(InetSocketAddress[] newMembers);

   MutableClusterView copy();

   boolean hasDamagedMembers();
   
   ServicePorts getListenPorts();
}
