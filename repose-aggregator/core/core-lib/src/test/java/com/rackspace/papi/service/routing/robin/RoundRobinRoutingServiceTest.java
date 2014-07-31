/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.service.routing.robin;

import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.core.service.config.ConfigurationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

@RunWith(Enclosed.class)
public class RoundRobinRoutingServiceTest {

   public static class WhenDeterminingRoutesWithRoundRobin {

      SystemModel systemModel;
      RoundRobinRoutingService robinRoutingService;

      @Before
      public void setUp() {

         final ConfigurationService configurationManager = null;
         robinRoutingService = new RoundRobinRoutingService(configurationManager);

         systemModel = new SystemModel();

         ReposeCluster domain = new ReposeCluster();
         domain.setFilters(mock(FilterList.class));
         domain.setId("reposeNode1");

         Node node1 = new Node();
         node1.setHostname("node1");
         node1.setHttpPort(8080);
         node1.setHttpsPort(0);
         
         
         Node node2 = new Node();
         node2.setHostname("node2");
         node2.setHttpPort(8181);
         node2.setHttpsPort(0);

         NodeList nodeList = new NodeList();
         nodeList.getNode().add(node1);
         nodeList.getNode().add(node2);

         domain.setNodes(nodeList);

         DestinationList destList = new DestinationList();


         domain.setDestinations(destList);
         systemModel.getReposeCluster().add(domain);
         
         robinRoutingService.setSystemModel(systemModel);
      }
      
      @Test
      public void shouldSwitchBetweenTwoRoutableNodes(){
         
         
         final Node node1 = robinRoutingService.getRoutableNode("reposeNode1");
         final Node node2 = robinRoutingService.getRoutableNode("reposeNode1");
         final Node node3 = robinRoutingService.getRoutableNode("reposeNode1");
         
         assertNotSame("Round Robin Routing Service will switch between the two configured destinations",node2, node1);
         assertEquals("Round Robin Routing Service will cycle back if all destinations have been returned",node3, node1);
      }
      
      @Test
      public void shouldReturnNullOnUnknownReposeNode(){
         final Node node1 = robinRoutingService.getRoutableNode("reposeNodeX");
         
         assertNull("No Routable Node", node1);
      }
      
    
   }
}
