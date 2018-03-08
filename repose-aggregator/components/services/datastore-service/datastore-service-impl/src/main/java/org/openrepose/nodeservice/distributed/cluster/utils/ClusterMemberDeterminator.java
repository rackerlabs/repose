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
package org.openrepose.nodeservice.distributed.cluster.utils;

import org.apache.commons.lang3.StringUtils;
import org.openrepose.core.services.datastore.distributed.config.DistributedDatastoreConfiguration;
import org.openrepose.core.services.datastore.distributed.config.Port;
import org.openrepose.core.systemmodel.config.Node;
import org.openrepose.core.systemmodel.config.ReposeCluster;
import org.openrepose.core.systemmodel.config.SystemModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

/**
 * A handful of convienience methods around determining the members of a cluster for a Distributed Datastore
 */
public class ClusterMemberDeterminator {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterMemberDeterminator.class);

    private ClusterMemberDeterminator() {
        // Prevent construction of this utility class.
    }

    /**
     * Get a list of all the cluster members for a specified cluster ID
     * You'll get the Port and inet address for those hosts.
     *
     * @param config
     * @param ddConfig
     * @param clusterId
     * @return
     */
    public static List<InetSocketAddress> getClusterMembers(SystemModel config, DistributedDatastoreConfiguration ddConfig, String clusterId) {
        final List<InetSocketAddress> cacheSiblings = new LinkedList<>();
        ReposeCluster cluster = getCurrentCluster(config.getReposeCluster(), clusterId);

        try {
            if (cluster != null) {
                for (Node node : cluster.getNodes().getNode()) {

                    final InetAddress hostAddress = InetAddress.getByName(node.getHostname());
                    final int port = getNodeDDPort(ddConfig, cluster.getId(), node.getId());
                    final InetSocketAddress hostSocketAddress = new InetSocketAddress(hostAddress, port);
                    cacheSiblings.add(hostSocketAddress);
                }
            }
        } catch (UnknownHostException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return cacheSiblings;
    }

    /**
     * Get the DD port that this node is going to use.
     *
     * @param config
     * @param clusterId
     * @param nodeId
     * @return
     */
    public static int getNodeDDPort(DistributedDatastoreConfiguration config, String clusterId, String nodeId) {
        LOG.debug("Finding DistDatastore Port for cluster: {} node: {}", clusterId, nodeId);
        int port = getDefaultDDPort(config, clusterId);
        for (Port curPort : config.getPortConfig().getPort()) {
            if (curPort.getCluster().equalsIgnoreCase(clusterId) && curPort.getNode().equalsIgnoreCase(nodeId)) {
                port = curPort.getPort();
                break;

            }
        }
        return port;
    }

    /**
     * The "default" dd port is always -1, because it is a required configuration. The port should never end up
     * being -1. I'm not sure why this is even here ...
     *
     * @param config
     * @param clusterId
     * @return
     */
    public static int getDefaultDDPort(DistributedDatastoreConfiguration config, String clusterId) {
        int port = -1;
        for (Port curPort : config.getPortConfig().getPort()) {
            if (curPort.getCluster().equalsIgnoreCase(clusterId) && "-1".equals(curPort.getNode())) {
                port = curPort.getPort();
            }
        }
        return port;
    }

    /**
     * Just gets the cluster object by cluster name without having to have a nodeID
     *
     * @param clusters
     * @param clusterId
     * @return
     */
    public static ReposeCluster getCurrentCluster(List<ReposeCluster> clusters, String clusterId) {
        for (ReposeCluster cluster : clusters) {
            if (StringUtils.equals(clusterId, cluster.getId())) {
                return cluster;
            }
        }
        return null;
    }
}
