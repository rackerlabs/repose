package com.rackspace.papi.service.routing.robin;

import com.rackspace.papi.model.Cluster;
import com.rackspace.papi.model.SystemModel;

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
