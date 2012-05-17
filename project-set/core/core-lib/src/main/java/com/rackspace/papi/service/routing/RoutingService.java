package com.rackspace.papi.service.routing;

import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.SystemModel;

public interface RoutingService {
   Node getRoutableNode(String domainId);
   void setSystemModel(SystemModel config);
}
