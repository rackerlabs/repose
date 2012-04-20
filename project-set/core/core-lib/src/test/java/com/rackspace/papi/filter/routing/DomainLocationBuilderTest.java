package com.rackspace.papi.filter.routing;

import com.rackspace.papi.model.DestinationDomain;
import com.rackspace.papi.model.DestinationList;
import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.model.DomainNodeList;
import com.rackspace.papi.model.ServiceDomain;
import com.rackspace.papi.service.routing.RoutingService;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class DomainLocationBuilderTest {

   public static class WhenRoutingToADomain {
      private DomainLocationBuilder instance;
      private RoutingService routingService;
      private DomainNode domainNode;
      private DestinationDomain dest;

      @Before
      public void setUp() {
         ServiceDomain domain = new ServiceDomain();
         domain.setId("domainId");

         domainNode = new DomainNode();
         domainNode.setHostname("destNode");
         domainNode.setHttpPort(8080);
         domainNode.setHttpsPort(8443);
         domainNode.setId("destNodeId");
         
         DomainNodeList list = new DomainNodeList();
         list.getNode().add(domainNode);
         domain.setServiceDomainNodes(list);
         
         routingService = mock(RoutingService.class);
         when(routingService.getRoutableNode(anyString())).thenReturn(domainNode);
         
         dest = new DestinationDomain();
         dest.setDefault(true);
         dest.setId("destId");
         dest.setProtocol("http");
         dest.setRootPath("/root");
         dest.setTargetDomain(domain);
         
         DestinationList destList = new DestinationList();
         destList.getTargetDomain().add(dest);
         
         domain.setDestinations(destList);
         
      }

      @Test
      public void shouldFindNodeAndBuildDestination() throws Exception {
         final String uri = "/context";
         
         instance = new DomainLocationBuilder(routingService, dest, uri);
         DestinationLocation build = instance.build();
         assertNotNull(build);
         assertNotNull(build.getUri());
         assertNotNull(build.getUrl());
         
         final String expectedPath = "/root" + uri;
         final String expectedUrl = dest.getProtocol() + "://" + domainNode.getHostname() + ":" + domainNode.getHttpPort() + expectedPath;
         
         assertEquals(expectedPath, build.getUri().getPath());
         assertEquals(expectedUrl, build.getUri().toString());
         assertEquals(expectedUrl, build.getUrl().toExternalForm());
      }

      @Test
      public void shouldReturnHttpsPort() throws Exception {
         final String uri = "/context";
         
         dest.setProtocol("https");
         
         instance = new DomainLocationBuilder(routingService, dest, uri);
         DestinationLocation build = instance.build();
         assertNotNull(build);
         assertNotNull(build.getUri());
         assertNotNull(build.getUrl());
         
         final String expectedPath = "/root" + uri;
         final String expectedUrl = dest.getProtocol() + "://" + domainNode.getHostname() + ":" + domainNode.getHttpsPort() + expectedPath;
         
         assertEquals(expectedPath, build.getUri().getPath());
         assertEquals(expectedUrl, build.getUri().toString());
         assertEquals(expectedUrl, build.getUrl().toExternalForm());
      }

      @Test
      public void shouldGetNullWhenNoRoutableNodeFound() throws Exception {
         final String uri = "/context";

         when(routingService.getRoutableNode(anyString())).thenReturn(null);
         
         instance = new DomainLocationBuilder(routingService, dest, uri);
         DestinationLocation build = instance.build();
         assertNull(build);
      }
   }
}
