package org.openrepose.core.services.routing;

import org.openrepose.core.systemmodel.Node;

public interface RoutingService {
   Node getRoutableNode(String domainId);
}
