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

import java.util.*;

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

    // @TODO: There will be only one cluster after REP-7314
    // @TODO: This should probably just be deleted as part of REP-7314
    @Deprecated
    public SystemModelInterrogator(String clusterId, String nodeId) {
        this.nodeId = nodeId;
    }

    public SystemModelInterrogator(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * For a given system model, get back a map of clusterIds to their NodeIds
     *
     * @param systemModel Takes a marshalled SystemModel to do work on
     * @return a Map of clusterIds to their NodeIds
     */
    public static Map<String, List<String>> allClusterNodes(SystemModel systemModel) {
        HashMap<String, List<String>> clusterNodes = new HashMap<>();
        // @TODO: There will be only one cluster after REP-7314
        for (ReposeCluster cluster : systemModel.getReposeCluster()) {
            LinkedList<String> nodes = new LinkedList<>();
            clusterNodes.put(cluster.getId(), nodes);
            for (Node node : cluster.getNodes().getNode()) {
                nodes.add(node.getId());
            }
        }

        return clusterNodes;
    }

    /**
     * Returns the ReposeCluster that the localhost belongs to.
     * Scoped by which cluster ID we're in, because there might be many
     */
    public Optional<ReposeCluster> getLocalCluster(SystemModel systemModel) {
        // @TODO: There will be only one cluster after REP-7314
        for (ReposeCluster cluster : systemModel.getReposeCluster()) {
            if (getLocalNode(cluster).isPresent()) {
                return Optional.of(cluster);
            }
        }

        return Optional.empty();
    }

    /**
     * Gets the local node from a specific cluster
     *
     * @param cluster
     * @return
     */
    public Optional<Node> getLocalNode(Cluster cluster) {
        Optional<Node> localNode = Optional.empty();
        for (Node node : cluster.getNodes().getNode()) {
            if (node.getId().equals(nodeId)) {
                localNode = Optional.of(node);
            }
        }
        return localNode;
    }

    /**
     * Returns the local node, based off the clusterID and nodeID provided
     *
     * @param systemModel the system model we're looking at
     * @return the Node jaxb element from the systemmodel
     */
    public Optional<Node> getLocalNode(SystemModel systemModel) {
        Optional<Node> localNode = Optional.empty();
        // @TODO: There will be only one cluster after REP-7314
        for (Cluster reposeCluster : systemModel.getReposeCluster()) {
            localNode = getLocalNode(reposeCluster);
        }

        return localNode;
    }

    /**
     * Returns an Optional<Service> if the service name requested is in the system model for your cluster
     *
     * @param systemModel The system model we're going to look at
     * @param serviceName The name of the service we want
     * @return Optional<Service>
     */
    public Optional<Service> getServiceForCluster(SystemModel systemModel, String serviceName) {
        Optional<ReposeCluster> cluster = getLocalCluster(systemModel);
        Optional<Service> found = Optional.empty();
        if (cluster.isPresent() && cluster.get().getServices() != null) {
            for (Service service : cluster.get().getServices().getService()) {
                if (service.getName().equalsIgnoreCase(serviceName)) {
                    found = Optional.of(service);
                }
            }
        }
        return found;
    }

    /**
     * Returns the default Destination for the cluster that the localhost belongs to.
     */
    public Optional<Destination> getDefaultDestination(SystemModel systemModel) {
        Optional<ReposeCluster> cluster = getLocalCluster(systemModel);

        if (!cluster.isPresent()) {
            return Optional.empty();
        }

        return getDefaultDestination(cluster);
    }


    private Optional<Destination> getDefaultDestination(Optional<ReposeCluster> cluster) {
        Optional<Destination> dest = Optional.empty();
        if (cluster.isPresent()) {
            List<Destination> destinations = new ArrayList<>();

            destinations.addAll(cluster.get().getDestinations().getEndpoint());
            destinations.addAll(cluster.get().getDestinations().getTarget());

            for (Destination destination : destinations) {
                if (destination.isDefault()) {
                    dest = Optional.of(destination);
                }
            }
        }
        return dest;
    }
}
