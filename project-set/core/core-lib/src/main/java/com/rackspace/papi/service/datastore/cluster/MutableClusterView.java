package com.rackspace.papi.service.datastore.cluster;

import com.rackspace.papi.domain.Port;
import java.net.InetSocketAddress;
import java.util.List;

public interface MutableClusterView extends ClusterView {

   void memberDamaged(InetSocketAddress address, String reason);

   void updateMembers(InetSocketAddress[] newMembers);

   MutableClusterView copy();

   boolean hasDamagedMembers();
   
   List<Port> getListenPorts();
}
