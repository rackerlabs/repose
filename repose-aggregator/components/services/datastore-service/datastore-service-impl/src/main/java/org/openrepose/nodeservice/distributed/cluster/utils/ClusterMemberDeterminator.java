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

import org.openrepose.core.services.datastore.distributed.config.DistributedDatastoreConfiguration;
import org.openrepose.core.services.datastore.distributed.config.Port;
import org.openrepose.core.systemmodel.config.Node;
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
     * @param config a system model configuration object
     * @param ddConfig a distributed datastore configuration object
     * @return
     */
    public static List<InetSocketAddress> getClusterMembers(SystemModel config, DistributedDatastoreConfiguration ddConfig) {
        final List<InetSocketAddress> cacheSiblings = new LinkedList<>();

        try {
            for (Node node : config.getNodes().getNode()) {
                final InetAddress hostAddress = InetAddress.getByName(node.getHostname());
                final int port = getNodeDDPort(ddConfig, node.getId());
                final InetSocketAddress hostSocketAddress = new InetSocketAddress(hostAddress, port);
                cacheSiblings.add(hostSocketAddress);
            }
        } catch (UnknownHostException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return cacheSiblings;
    }

    /**
     * Get the DD port that this node is going to use.
     *
     * @param config a distributed datastore configuration object
     * @param nodeId a node identifier
     * @return the node port if present, otherwise the default port
     */
    public static int getNodeDDPort(DistributedDatastoreConfiguration config, String nodeId) {
        LOG.debug("Finding DistDatastore Port for node: {}", nodeId);
        int port = getDefaultDDPort(config);
        for (Port curPort : config.getPortConfig().getPort()) {
            if (curPort.getNode().equals(nodeId)) {
                port = curPort.getPort();
                break;

            }
        }
        return port;
    }

    /**
     * The default port is defined as a port with a node identifier set to "-1".
     * The configuration schema asserts that at most one default port is defined.
     * Therefore, we can return the first port mapped to a node identified by "-1".
     *
     * @param config a distributed datastore configuration object
     * @return the default port if present, otherwise -1
     */
    private static int getDefaultDDPort(DistributedDatastoreConfiguration config) {
        int port = -1;
        for (Port curPort : config.getPortConfig().getPort()) {
            if ("-1".equals(curPort.getNode())) {
                port = curPort.getPort();
                break;
            }
        }
        return port;
    }
}
