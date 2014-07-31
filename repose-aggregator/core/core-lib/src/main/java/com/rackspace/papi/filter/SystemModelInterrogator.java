package com.rackspace.papi.filter;

import com.google.common.base.Optional;
import com.rackspace.papi.commons.util.net.NetworkInterfaceProvider;
import com.rackspace.papi.commons.util.net.NetworkNameResolver;
import com.rackspace.papi.commons.util.net.StaticNetworkInterfaceProvider;
import com.rackspace.papi.commons.util.net.StaticNetworkNameResolver;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper class used to inspect a system model. Methods are provided to determine the relation between the localhost
 * and the system model.
 */
public class SystemModelInterrogator {
    private static final Logger LOG = LoggerFactory.getLogger(SystemModelInterrogator.class);

    private final NetworkInterfaceProvider networkInterfaceProvider;
    private final NetworkNameResolver nameResolver;
    private final String localClusterId;
    private final String localNodeId;

    public SystemModelInterrogator(ReposeInstanceInfo rii) {
        this(rii.getClusterId(), rii.getNodeId());
    }

    public SystemModelInterrogator(String localClusterId, String localNodeId) {
        this.localClusterId = localClusterId;
        this.localNodeId = localNodeId;

        this.nameResolver = StaticNetworkNameResolver.getInstance();
        this.networkInterfaceProvider = StaticNetworkInterfaceProvider.getInstance();
    }

    /**
     * Returns the ReposeCluster that the localhost belongs to.
     */
    public Optional<ReposeCluster> getLocalCluster(SystemModel systemModel) {
        for (ReposeCluster cluster : systemModel.getReposeCluster()) {
            if (getLocalNodeForPorts(cluster, getPortsForNode(determineLocalNode(systemModel))).isPresent()) {
                return Optional.of(cluster);
            }
        }

        return Optional.absent();
    }

    /**
     * Returns the Node that matches the localhost.
     */
    public Optional<Node> getLocalNode(SystemModel systemModel) {
        for (ReposeCluster cluster : systemModel.getReposeCluster()) {
            Optional<Node> node = getLocalNodeForPorts(cluster, getPortsForNode(determineLocalNode(systemModel)));

            if (node.isPresent()) {
                return node;
            }
        }

        return Optional.absent();
    }

    /**
     * Returns the default Destination for the cluster that the localhost belongs to.
     */
    public Optional<Destination> getDefaultDestination(SystemModel systemModel) {
        Optional<ReposeCluster> cluster = getLocalCluster(systemModel);

        if (!cluster.isPresent()) {
            return Optional.absent();
        }

        return getDefaultDestination(cluster.get());
    }

    private boolean hasLocalInterface(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }

        boolean result = false;

        try {
            final InetAddress hostAddress = nameResolver.lookupName(node.getHostname());
            result = networkInterfaceProvider.hasInterfaceFor(hostAddress);
        } catch (UnknownHostException uhe) {
            LOG.error("Unable to look up network host name. Reason: " + uhe.getMessage(), uhe);
        } catch (SocketException socketException) {
            LOG.error(socketException.getMessage(), socketException);
        }

        return result;
    }

    private List<Port> getPortsList(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }

        List<Port> portList = new ArrayList<Port>();

        // TODO Model: use constants or enum for possible protocols
        if (node.getHttpPort() > 0) {
            portList.add(new Port("http", node.getHttpPort()));
        }

        if (node.getHttpsPort() > 0) {
            portList.add(new Port("https", node.getHttpsPort()));
        }

        return portList;
    }

    private Optional<Node> getLocalNodeForPorts(Cluster cluster, List<Port> ports) {
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster cannot be null");
        }

        if (ports.isEmpty()) {
            return Optional.absent();
        }

        for (Node node : cluster.getNodes().getNode()) {
            List<Port> hostPorts = getPortsList(node);

            if (hostPorts.equals(ports) && hasLocalInterface(node)) {
                return Optional.of(node);
            }
        }

        return Optional.absent();
    }

    private Optional<Destination> getDefaultDestination(ReposeCluster cluster) {
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster cannot be null");
        }

        List<Destination> destinations = new ArrayList<Destination>();

        destinations.addAll(cluster.getDestinations().getEndpoint());
        destinations.addAll(cluster.getDestinations().getTarget());

        for (Destination destination : destinations) {
            if (destination.isDefault()) {
                return Optional.of(destination);
            }
        }

        return Optional.absent();
    }

    /**
     * Get the ports from the system model and return them as a list
     *
     * @param reposeNode
     * @return
     */
    public List<Port> getPortsForNode(final Node reposeNode) {
        final ArrayList<Port> ports = new ArrayList<>();

        if (reposeNode != null) {
            if (reposeNode.getHttpPort() != 0) {
                ports.add(new Port("http", reposeNode.getHttpPort()));
            } else {
                LOG.error("Http service port not specified for Repose Node {}", reposeNode.getId());
            }

            if (reposeNode.getHttpsPort() != 0) {
                ports.add(new Port("https", reposeNode.getHttpsPort()));
            } else {
                LOG.info("Https service port not specified for Repose Node {}", reposeNode.getId());
            }
        }
        return ports;
    }

    public Node determineLocalNode(final SystemModel config) {
        for (ReposeCluster cluster : config.getReposeCluster()) {
            if (cluster.getId().equals(localClusterId)) {
                for (Node node : cluster.getNodes().getNode()) {
                    if (node.getId().equals(localNodeId)) {
                        return node;
                    }
                }
            }
        }
        return null;
    }


}