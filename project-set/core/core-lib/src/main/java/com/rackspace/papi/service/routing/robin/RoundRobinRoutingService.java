package com.rackspace.papi.service.routing.robin;

import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.routing.RoutingService;

public class RoundRobinRoutingService implements RoutingService {
   private final Clusters domains;

   public RoundRobinRoutingService(SystemModel config) {
      this.domains = new Clusters(config);
   }
   
   @Override
   public Node getRoutableNode(String domainId) {
      ClusterWrapper domain = domains.getDomain(domainId);
      if (domain != null) {
         return domain.getNextNode();
      }
      
      return null;
   }
}
