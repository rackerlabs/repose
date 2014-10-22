package org.openrepose.core.filter;

import com.google.common.base.Optional;
import org.openrepose.core.systemmodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A helper class used to inspect a system model. Methods are provided to determine the relation between the given
 * ClusterID and NodeID and the system model.
 * <p/>
 * This used to work based on the localh host and which port it was running on. Given the new spring stuff, we can
 * give each individual node running (in valve or in war) the clusterID and nodeID, so this is really just convenience
 * methods about getting information about the current node from the system model.
 */
public class SystemModelInterrogator {
    private static final Logger LOG = LoggerFactory.getLogger(SystemModelInterrogator.class);

    private final String clusterId;
    private final String nodeId;

    public SystemModelInterrogator(String clusterId, String nodeId) {
        this.clusterId = clusterId;
        this.nodeId = nodeId;
    }

    /**
     * Returns the ReposeCluster that the localhost belongs to.
     */
    public Optional<ReposeCluster> getLocalCluster(SystemModel systemModel) {
        for (ReposeCluster cluster : systemModel.getReposeCluster()) {
            if (getLocalNode(systemModel).isPresent()) {
                return Optional.of(cluster);
            }
        }

        return Optional.absent();
    }

    /**
     * Returns the local node, based off the clusterID and nodeID provided
     *
     * @param systemModel the system model we're looking at
     * @return the Node jaxb element from the systemmodel
     */
    public Optional<Node> getLocalNode(SystemModel systemModel) {
        Optional<Node> localNode = Optional.absent();
        for (Cluster reposeCluster : systemModel.getReposeCluster()) {
            if (reposeCluster.getId().equals(clusterId)) {
                for (Node node : reposeCluster.getNodes().getNode()) {
                    if (node.getId().equals(nodeId)) {
                        localNode = Optional.of(node);
                    }
                }
            }
        }

        return localNode;
    }

    /**
     * Returns the default Destination for the cluster that the localhost belongs to.
     */
    public Optional<Destination> getDefaultDestination(SystemModel systemModel) {
        Optional<ReposeCluster> cluster = getLocalCluster(systemModel);

        if (!cluster.isPresent()) {
            return Optional.absent();
        }

        return getDefaultDestination(cluster);
    }

    private Optional<Destination> getDefaultDestination(Optional<ReposeCluster> cluster) {
        Optional<Destination> dest = Optional.absent();
        if (cluster.isPresent()) {
            List<Destination> destinations = new ArrayList<Destination>();

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