package com.rackspace.papi.service.routing.robin;

import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.model.ServiceDomain;
import java.util.ArrayList;
import java.util.List;

public class ServiceDomainWrapper {
   private final List<DomainNode> nodes;
   private final int nodeCount;
   private int currentIndex = 0;
   
   public ServiceDomainWrapper(ServiceDomain domain) {
      if (domain == null) {
         throw new IllegalArgumentException("Domain cannot be null");
      }
      
      this.nodes = domain.getServiceDomainNodes() != null? domain.getServiceDomainNodes().getNode(): new ArrayList<DomainNode>();
      this.nodeCount = nodes.size();
   }
   
   public DomainNode getNextNode() {
      synchronized(nodes) {
         return getNode(currentIndex++);
      }
   }
   
   public DomainNode getNode(int index) {
      return nodeCount > 0 && index >= 0? nodes.get(index % nodeCount): null;
   }
   
}
