package org.openrepose.nodeservice.routing;

import org.openrepose.core.systemmodel.Node;
import org.openrepose.core.systemmodel.SystemModel;

public interface RoutingService {
   Node getRoutableNode(String domainId);
   void setSystemModel(SystemModel config);
}
