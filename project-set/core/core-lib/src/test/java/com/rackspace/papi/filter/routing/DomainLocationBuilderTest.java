package com.rackspace.papi.filter.routing;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.model.DestinationCluster;
import com.rackspace.papi.model.DestinationList;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.NodeList;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.service.routing.RoutingService;
import javax.servlet.http.HttpServletRequest;
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
      private Node domainNode;
      private DestinationCluster dest;
      private HttpServletRequest request;

      @Before
      public void setUp() {
         ReposeCluster domain = new ReposeCluster();
         domain.setId("domainId");

         domainNode = new Node();
         domainNode.setHostname("destNode");
         domainNode.setHttpPort(8080);
         domainNode.setHttpsPort(8443);
         domainNode.setId("destNodeId");
         
         NodeList list = new NodeList();
         list.getNode().add(domainNode);
         domain.setNodes(list);
         
         routingService = mock(RoutingService.class);
         when(routingService.getRoutableNode(anyString())).thenReturn(domainNode);
         
         request = mock(HttpServletRequest.class);
         when(request.getQueryString()).thenReturn(null);
         
         dest = new DestinationCluster();
         dest.setDefault(true);
         dest.setId("destId");
         dest.setProtocol("http");
         dest.setRootPath("/root");
         dest.setCluster(domain);
         
         DestinationList destList = new DestinationList();
         destList.getTarget().add(dest);
         
         domain.setDestinations(destList);
         
      }

      @Test
      public void shouldFindNodeAndBuildDestination() throws Exception {
         final String uri = "/context";
         
         instance = new DomainLocationBuilder(routingService, dest, uri,request);
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
         
         instance = new DomainLocationBuilder(routingService, dest, uri,request);
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
         
         instance = new DomainLocationBuilder(routingService, dest, uri,request);
         DestinationLocation build = instance.build();
         assertNull(build);
      }
   }
}
