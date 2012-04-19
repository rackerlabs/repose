package com.rackspace.papi.service.routing;

import com.rackspace.papi.model.DomainNode;

public interface RoutingService {
   public DomainNode getRoutableNode(String domainId);
}
