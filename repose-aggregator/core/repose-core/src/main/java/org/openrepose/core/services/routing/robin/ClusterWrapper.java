/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.routing.robin;

import org.openrepose.core.systemmodel.config.Cluster;
import org.openrepose.core.systemmodel.config.Node;

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

        this.nodes = domain.getNodes() != null ? domain.getNodes().getNode() : new ArrayList<>();
        this.nodeCount = nodes.size();
    }

    public Node getNextNode() {
        synchronized (nodes) {
            // Reset the currentIndex to prevent int overflow
            if (currentIndex == Integer.MAX_VALUE) {
                currentIndex = 0;
            }

            return getNode(currentIndex++);
        }
    }

    public Node getNode(int index) {
        return nodeCount > 0 && index >= 0 ? nodes.get(index % nodeCount) : null;
    }
}
