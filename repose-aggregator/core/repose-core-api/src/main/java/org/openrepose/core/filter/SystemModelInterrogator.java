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
package org.openrepose.core.filter;

import org.openrepose.core.systemmodel.config.*;

import java.util.Collections;
import java.util.Optional;

/**
 * A helper class used to inspect a system model. Methods are provided to determine the relation between the given
 * ClusterID and NodeID and the system model.
 * <p/>
 * This used to work based on the local host and which port it was running on. Given the new spring stuff, we can
 * give each individual node running (in valve or in war) the clusterID and nodeID, so this is really just convenience
 * methods about getting information about the current node from the system model.
 */
public class SystemModelInterrogator {
    private final String nodeId;

    public SystemModelInterrogator(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * If the {@code nodeId} is listed in the system model, returns the node object.
     * Otherwise, returns an empty Optional.
     *
     * @param systemModel the system model to inspect
     * @return a node object
     */
    public Optional<Node> getNode(SystemModel systemModel) {
        return systemModel.getNodes().getNode().stream()
            .filter(node -> node.getId().equals(nodeId))
            .findAny();
    }

    /**
     * If the desired service is listed in the system model, returns the service object.
     * Otherwise, returns an empty Optional.
     *
     * @param systemModel the system model to inspect
     * @param serviceName the desired service name
     * @return the service object
     */
    public static Optional<Service> getService(SystemModel systemModel, String serviceName) {
        return Optional.ofNullable(systemModel.getServices())
            .map(ServicesList::getService)
            .orElseGet(Collections::emptyList)
            .stream()
            .filter(service -> service.getName().equalsIgnoreCase(serviceName))
            .findAny();
    }

    /**
     * If a default destination exists, returns that destination.
     * Otherwise, returns an empty Optional.
     * <p>
     * The system model schema asserts that exactly one default destination must exist,
     * so this code assumes that assertion holds.
     *
     * @param systemModel the system model to inspect
     * @return the default destination
     */
    public static Optional<Destination> getDefaultDestination(SystemModel systemModel) {
        return systemModel.getDestinations().getEndpoint().stream()
            .filter(Destination::isDefault)
            .findAny();
    }
}
