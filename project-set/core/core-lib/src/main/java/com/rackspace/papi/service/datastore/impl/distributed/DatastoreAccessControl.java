package com.rackspace.papi.service.datastore.impl.distributed;

import java.net.InetAddress;
import java.util.List;


public class DatastoreAccessControl {

   private final List<InetAddress> allowedHosts;
   private final boolean allowAll;

   public DatastoreAccessControl(List<InetAddress> allowedHosts, boolean allowAll) {
      this.allowedHosts = allowedHosts;
      this.allowAll = allowAll;
   }

   public boolean shouldAllowAll() {
      return allowAll;
   }

   public List<InetAddress> getAllowedHosts() {
      return allowedHosts;
   }
}
