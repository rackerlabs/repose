package com.rackspace.papi.service.routing.robin;

import com.rackspace.papi.model.Cluster;
import com.rackspace.papi.model.Node;

import java.util.ArrayList;
import java.util.List;

public class ClusterWrapper {
   private final List<Node> nodes;
   private final int nodeCount;
   private int currentIndex = 0;
   
   public ClusterWrapper(Cluster domain) {
      if (domain == null) {
         throw new IllegalArgumentException("Domain cannot be null");
      }
      
      this.nodes = domain.getNodes() != null? domain.getNodes().getNode(): new ArrayList<Node>();
      this.nodeCount = nodes.size();
   }
   
   public Node getNextNode() {
      synchronized(nodes) {
         return getNode(currentIndex++);
      }
   }
   
   public Node getNode(int index) {
      return nodeCount > 0 && index >= 0? nodes.get(index % nodeCount): null;
   }
   
}
