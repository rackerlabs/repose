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

import org.openrepose.core.services.datastore.DatastoreAccessControl;
import org.openrepose.core.services.datastore.distributed.config.DistributedDatastoreConfiguration;
import org.openrepose.core.services.datastore.distributed.config.HostAccessControl;
import org.openrepose.core.systemmodel.config.Node;
import org.openrepose.core.systemmodel.config.SystemModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

/**
 * A collection of convenience methods around determining the Access list for a cluster for a dist datastore.
 */
public class AccessListDeterminator {

    private static final Logger LOG = LoggerFactory.getLogger(AccessListDeterminator.class);

    private AccessListDeterminator() {
        // Prevent construction of this utility class.
    }

    public static DatastoreAccessControl getAccessList(DistributedDatastoreConfiguration config, List<InetAddress> clusterMembers) {

        boolean allowAll = config.getAllowedHosts().isAllowAll();

        //Automatically Adds all cluster members to access list
        List<InetAddress> hostAccessList = new LinkedList<>(clusterMembers);

        if (!allowAll) {
            hostAccessList.addAll(getConfiguredAllowedHosts(config));
        }

        if (allowAll) {
            LOG.info("The distributed datastore component is configured in allow-all mode meaning that any host can access, store and delete cached objects.");
        } else {
            LOG.info("The distributed datastore component has access controls configured meaning that only the configured hosts and cluster members "
                    + "can access, store and delete cached objects.");
        }
        LOG.debug("Allowed Hosts: " + hostAccessList.toString());


        return new DatastoreAccessControl(hostAccessList, allowAll);

    }

    private static List<InetAddress> getConfiguredAllowedHosts(DistributedDatastoreConfiguration curDistributedDatastoreConfiguration) {

        final List<InetAddress> configuredAllowedHosts = new LinkedList<>();

        for (HostAccessControl host : curDistributedDatastoreConfiguration.getAllowedHosts().getAllow()) {
            try {
                final InetAddress hostAddress = InetAddress.getByName(host.getHost());
                configuredAllowedHosts.add(hostAddress);
            } catch (UnknownHostException e) {
                LOG.warn("Unable to resolve host: " + host.getHost(), e);
            }
        }

        return configuredAllowedHosts;
    }

    public static List<InetAddress> getClusterMembers(SystemModel config) {

        final List<InetAddress> reposeClusterMembers = new LinkedList<>();

        for (Node node : config.getNodes().getNode()) {
            try {
                final InetAddress hostAddress = InetAddress.getByName(node.getHostname());
                reposeClusterMembers.add(hostAddress);
            } catch (UnknownHostException e) {
                LOG.warn("Unable to resolve host: " + node.getHostname() + "for Node " + node.getId(), e);
            }

        }

        return reposeClusterMembers;
    }
}
