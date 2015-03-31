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

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.core.services.datastore.DatastoreAccessControl;
import org.openrepose.core.services.datastore.distributed.config.*;
import org.openrepose.core.systemmodel.*;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class AccessListDeterminatorTest {

    public static class WhenTestingAccessListDeterminator {

        private SystemModel sysConfig;
        private DistributedDatastoreConfiguration ddConfig;
        private List<ReposeCluster> clusters;
        private ReposeCluster cluster;
        private Node node1, node2;
        private NodeList nodeList;
        private List<Filter> filters;
        private FilterList filterList;
        private HostAccessControlList hacl;
        private boolean isAllowed;
        private HostAccessControl ctrl;
        private PortConfiguration portConfig;
        private Port node1Port;

        @Before
        public void setUp() {

            filters = new ArrayList<Filter>();
            filterList = new FilterList();
            filterList.getFilter().addAll(filters);

            node1 = new Node();
            node1.setHttpPort(8888);
            node1.setHostname("127.0.0.1");
            node1.setId("node1");
            nodeList = new NodeList();
            nodeList.getNode().add(node1);


            node2 = new Node();
            node2.setHttpPort(8889);
            node2.setHostname("127.0.0.1");
            node2.setId("node2");
            nodeList.getNode().add(node2);

            cluster = new ReposeCluster();
            cluster.setFilters(filterList);
            cluster.setId("reposeCluster");
            cluster.setNodes(nodeList);

            sysConfig = new SystemModel();
            sysConfig.getReposeCluster().add(cluster);

            node1Port = new Port();
            node1Port.setCluster("reposeCluster");

            isAllowed = false;

            ctrl = new HostAccessControl();
            ctrl.setHost("127.0.0.1");

            hacl = new HostAccessControlList();
            hacl.setAllowAll(isAllowed);
            hacl.getAllow().add(ctrl);

            ddConfig = new DistributedDatastoreConfiguration();
            ddConfig.setAllowedHosts(hacl);
            ddConfig.setPortConfig(portConfig);


        }

        @Test
        public void shouldGetClusterMembers() {

            List<InetAddress> clusterMembers = AccessListDeterminator.getClusterMembers(sysConfig, "reposeCluster");

            assertTrue("Should have two cluster members", clusterMembers.size() == 2);
        }

        @Test
        public void shouldGetAccessList() {

            List<InetAddress> clusterMembers = AccessListDeterminator.getClusterMembers(sysConfig, "reposeCluster");

            DatastoreAccessControl allowedHosts = AccessListDeterminator.getAccessList(ddConfig, clusterMembers);

            assertFalse("Should not allow all", allowedHosts.shouldAllowAll());
        }
    }
}
