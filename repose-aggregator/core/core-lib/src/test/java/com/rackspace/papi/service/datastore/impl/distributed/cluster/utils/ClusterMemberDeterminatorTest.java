/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.service.datastore.impl.distributed.cluster.utils;

import com.rackspace.papi.model.Filter;
import com.rackspace.papi.model.FilterList;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.NodeList;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.datastore.impl.distributed.config.DistributedDatastoreConfiguration;
import com.rackspace.papi.service.datastore.impl.distributed.config.HostAccessControl;
import com.rackspace.papi.service.datastore.impl.distributed.config.HostAccessControlList;
import com.rackspace.papi.service.datastore.impl.distributed.config.Port;
import com.rackspace.papi.service.datastore.impl.distributed.config.PortConfiguration;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class ClusterMemberDeterminatorTest {

   public static class WhenDeterminingClusterMembers {

      private SystemModel sysConfig;
      private DistributedDatastoreConfiguration ddConfig;
      private List<ReposeCluster> clusters;
      private ReposeCluster cluster1, cluster2;
      private Node node1, node2;
      private NodeList nodeList;
      private List<Filter> filters;
      private FilterList filterList;
      private HostAccessControlList hacl;
      private boolean isAllowed;
      private HostAccessControl ctrl;
      private PortConfiguration portConfig;
      private Port node1Port, node2Port;

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

         cluster1 = new ReposeCluster();
         cluster1.setFilters(filterList);
         cluster1.setId("reposeCluster");
         cluster1.setNodes(nodeList);

         cluster2 = new ReposeCluster();
         cluster2.setFilters(filterList);
         cluster2.setId("otherReposeCluster");


         sysConfig = new SystemModel();
         sysConfig.getReposeCluster().add(cluster1);

         node1Port = new Port();
         node1Port.setCluster("reposeCluster");
         node1Port.setPort(9999);


         node2Port = new Port();
         node2Port.setCluster("reposeCluster");
         node2Port.setNode("node2");
         node2Port.setPort(3333);

         portConfig = new PortConfiguration();
         portConfig.getPort().add(node1Port);
         portConfig.getPort().add(node2Port);

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
      public void whenDeterminingCurrentCluster() {

         ReposeCluster getCluster = ClusterMemberDeterminator.getCurrentCluster(sysConfig.getReposeCluster(), "reposeCluster");

         assertTrue("should retrieve cluster", getCluster.getId().equals("reposeCluster"));

      }

      @Test
      public void whenRetrievingNonExistantCluster() {

         ReposeCluster getCluster = ClusterMemberDeterminator.getCurrentCluster(sysConfig.getReposeCluster(), "nonExistantCluster");

         assertNull("No cluster retrieved", getCluster);
      }

      @Test
      public void whenRetrievingDDPort() {

         int ddPort = ClusterMemberDeterminator.getNodeDDPort(ddConfig, "reposeCluster", "node1");
         int ddPort2 = ClusterMemberDeterminator.getNodeDDPort(ddConfig, "reposeCluster", "node2");

         assertTrue("Should determine proper dd port 1", ddPort == 9999);
         assertTrue("Should determine proper dd port 2", ddPort2 == 3333);

      }
      
      @Test
      public void whenRetrievingClusterMembers(){
         
         List<InetSocketAddress> clusterView = ClusterMemberDeterminator.getClusterMembers(sysConfig, ddConfig, "reposeCluster");
         
         assertTrue("Cluster has 2 repose nodes", clusterView.size() == 2);
      }
   }
}