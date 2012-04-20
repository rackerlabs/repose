package com.rackspace.papi.service.routing;

import com.rackspace.papi.model.DomainNode;

public interface RoutingService {
   DomainNode getRoutableNode(String domainId);
}
