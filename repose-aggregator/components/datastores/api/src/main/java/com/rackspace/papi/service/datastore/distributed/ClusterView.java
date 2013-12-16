package com.rackspace.papi.service.datastore.distributed;

import java.net.InetSocketAddress;
import java.net.SocketException;

public interface ClusterView {

   InetSocketAddress[] members();

   boolean isLocal(InetSocketAddress addr) throws SocketException;

   void memberDamaged(InetSocketAddress address, String reason);

   void updateMembers(InetSocketAddress[] newMembers);

   ClusterView copy();

   boolean hasDamagedMembers();
}
