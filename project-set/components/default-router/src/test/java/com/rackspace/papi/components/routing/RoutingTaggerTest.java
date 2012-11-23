package com.rackspace.papi.components.routing;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.net.NetworkInterfaceProvider;
import com.rackspace.papi.commons.util.net.NetworkNameResolver;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.model.*;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.MalformedURLException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author malconis
 */
public class RoutingTaggerTest {

   SystemModelInterrogator interrogator;
   HttpServletRequest request;
   ReadableHttpServletResponse response;
   RoutingTagger routingTagger;
   SystemModel systemModel;
   String myHostName, requestUri, nextHostName;
   DestinationEndpoint defaultDest;

   private ServicePorts getHttpPortList(int port) {
      ServicePorts ports = new ServicePorts();
      ports.add(new Port("http", port));
      return ports;
   }

   @Before
   public void setUp() throws Exception {
      requestUri = "/mock/request/uri";

      request = mock(HttpServletRequest.class);
      response = mock(ReadableHttpServletResponse.class);
      systemModel = new SystemModel();

      when(request.getRequestURI()).thenReturn(requestUri);

      NetworkNameResolver resolver = mock(NetworkNameResolver.class);
      when(resolver.lookupName(eq("localhost"))).thenReturn(InetAddress.getByAddress("localhost", new byte[]{127, 0, 0, 1}));

      NetworkInterfaceProvider interfaceProvider = mock(NetworkInterfaceProvider.class);
      when(interfaceProvider.hasInterfaceFor(any(InetAddress.class))).thenReturn(Boolean.TRUE);

      interrogator = new SystemModelInterrogator(resolver, interfaceProvider, getHttpPortList(8080));

      ReposeCluster domain = new ReposeCluster();
      domain.setFilters(mock(FilterList.class));

      Node node = new Node();
      node.setHostname("localhost");
      node.setHttpPort(8080);
      node.setHttpsPort(0);

      NodeList nodeList = new NodeList();
      nodeList.getNode().add(node);
      
      domain.setNodes(nodeList);

      defaultDest = new DestinationEndpoint();
      defaultDest.setId("default");
      defaultDest.setHostname(nextHostName);
      defaultDest.setPort(8081);
      defaultDest.setProtocol("http");
      defaultDest.setDefault(Boolean.TRUE);

      DestinationList destList = new DestinationList();
      destList.getEndpoint().add(defaultDest);
      domain.setDestinations(destList);

      systemModel.getReposeCluster().add(domain);
   }

   /**
    * Test of handleRequest method, of class RoutingTagger.
    */
   @Test
   public void shouldNotChangeNextRouteWhenValueIsPresent() {
      routingTagger = new RoutingTagger();
      routingTagger.setDestination(defaultDest);
      when(request.getHeader(PowerApiHeader.NEXT_ROUTE.toString())).thenReturn("http://mockendservice.com:8082");
      FilterDirector result = routingTagger.handleRequest(request, response);
      assertTrue("Should not change route destination", request.getHeader(PowerApiHeader.NEXT_ROUTE.toString()).equals("http://mockendservice.com:8082"));

   }

   @Test
   public void shouldRouteToNextNonLocalHost() throws MalformedURLException {
      routingTagger = new RoutingTagger();
      routingTagger.setDestination(defaultDest);

      FilterDirector result = routingTagger.handleRequest(request, response);

      assertFalse("Should have destination", result.getDestinations().isEmpty());
      assertEquals("Should have default destination", "default", result.getDestinations().get(0).getDestinationId());
      // Filters no longer set NEXT_ROUTE.  Filters add to the destination list
      //assertTrue("Should route to next non-localhost host", result.requestHeaderManager().headersToAdd().get(PowerApiHeader.NEXT_ROUTE.toString().toLowerCase()).contains(nextRoute));

   }
}
