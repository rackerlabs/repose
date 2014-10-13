package org.openrepose.core.services.routing.robin;

import org.openrepose.core.systemmodel.Cluster;
import org.openrepose.core.systemmodel.SystemModel;

import java.util.HashMap;
import java.util.Map;

public class Clusters {
   private final Map<String, ClusterWrapper> domains;
   
   public Clusters(SystemModel config) {
      domains = new HashMap<String, ClusterWrapper>();
      
      for (Cluster domain: config.getReposeCluster()) {
         domains.put(domain.getId(), new ClusterWrapper(domain));
      }
      for (Cluster domain: config.getServiceCluster()) {
         domains.put(domain.getId(), new ClusterWrapper(domain));
      }
   }
   
   public ClusterWrapper getDomain(String id) {
      return domains.get(id);
   }
}
