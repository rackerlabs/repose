/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.service.datastore.impl.distributed.cluster.utils;

import com.rackspace.papi.model.Cluster;
import com.rackspace.papi.model.Filter;
import com.rackspace.papi.model.FilterList;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.NodeList;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.datastore.impl.distributed.DatastoreAccessControl;
import java.net.InetAddress;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import com.rackspace.papi.service.datastore.impl.distributed.config.DistributedDatastoreConfiguration;
import com.rackspace.papi.service.datastore.impl.distributed.config.HostAccessControl;
import com.rackspace.papi.service.datastore.impl.distributed.config.HostAccessControlList;
import com.rackspace.papi.service.datastore.impl.distributed.config.Port;
import com.rackspace.papi.service.datastore.impl.distributed.config.PortConfiguration;
import java.util.ArrayList;
import org.junit.Ignore;

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