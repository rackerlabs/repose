package com.rackspace.papi.service.routing.robin;

import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.service.routing.RoutingService;

public class RoundRobinRoutingService implements RoutingService {
   private final PowerProxy config;
   private final ServiceDomains domains;

   public RoundRobinRoutingService(PowerProxy config) {
      this.config = config;
      this.domains = new ServiceDomains(config);
   }
   
   @Override
   public DomainNode getRoutableNode(String domainId) {
      ServiceDomainWrapper domain = domains.getDomain(domainId);
      if (domain != null) {
         return domain.getNextNode();
      }
      
      return null;
   }
}
