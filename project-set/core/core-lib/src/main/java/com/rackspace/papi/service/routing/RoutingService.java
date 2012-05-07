package com.rackspace.papi.service.routing;

import com.rackspace.papi.model.Node;

public interface RoutingService {
   Node getRoutableNode(String domainId);
}
