/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.core.services.routing.robin;

import org.openrepose.core.systemmodel.Cluster;
import org.openrepose.core.systemmodel.Node;

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
