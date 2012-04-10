/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.routing;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.domain.HostUtilities;
import com.rackspace.papi.filter.logic.FilterDirector;
import java.net.MalformedURLException;
import javax.servlet.http.HttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.commons.util.net.NetUtilities;
import com.rackspace.papi.commons.util.net.NetworkInterfaceProvider;
import com.rackspace.papi.commons.util.net.NetworkNameResolver;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DestinationEndpoint;
import com.rackspace.papi.model.DestinationList;
import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.model.DomainNodeList;


import com.rackspace.papi.model.FilterList;
import com.rackspace.papi.model.ServiceDomain;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
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
   PowerProxy systemModel;
   String myHostName, requestUri, nextHostName;
   DestinationEndpoint defaultDest;

   private List<Port> getHttpPortList(int port) {
      List<Port> ports = new ArrayList<Port>();
      ports.add(new Port("http", port));
      return ports;
   }

   @Before
   public void setUp() throws Exception {
      requestUri = "/mock/request/uri";

      request = mock(HttpServletRequest.class);
      response = mock(ReadableHttpServletResponse.class);
      systemModel = new PowerProxy();

      when(request.getRequestURI()).thenReturn(requestUri);

      NetworkNameResolver resolver = mock(NetworkNameResolver.class);
      when(resolver.lookupName(eq("localhost"))).thenReturn(InetAddress.getByAddress("localhost", new byte[]{127, 0, 0, 1}));

      NetworkInterfaceProvider interfaceProvider = mock(NetworkInterfaceProvider.class);
      when(interfaceProvider.hasInterfaceFor(any(InetAddress.class))).thenReturn(Boolean.TRUE);

      interrogator = new SystemModelInterrogator(resolver, interfaceProvider, systemModel, getHttpPortList(8080));

      ServiceDomain domain = new ServiceDomain();
      domain.setFilters(mock(FilterList.class));

      DomainNode node = new DomainNode();
      node.setHostname("localhost");
      node.setHttpPort(8080);
      node.setHttpsPort(0);

      DomainNodeList nodeList = new DomainNodeList();
      nodeList.getNode().add(node);
      
      domain.setServiceDomainNodes(nodeList);

      defaultDest = new DestinationEndpoint();
      defaultDest.setHostname(nextHostName);
      defaultDest.setPort(8081);
      defaultDest.setProtocol("http");
      defaultDest.setDefault(Boolean.TRUE);

      DestinationList destList = new DestinationList();
      destList.getEndpoint().add(defaultDest);
      domain.setDestinations(destList);

      systemModel.getServiceDomain().add(domain);
   }

   /**
    * Test of handleRequest method, of class RoutingTagger.
    */
   @Test
   public void shouldNotChangeNextRouteWhenValueIsPresent() {
      routingTagger = new RoutingTagger(interrogator);
      when(request.getHeader(PowerApiHeader.NEXT_ROUTE.toString())).thenReturn("http://mockendservice.com:8082");
      FilterDirector result = routingTagger.handleRequest(request, response);
      assertTrue("Should not change route destination", request.getHeader(PowerApiHeader.NEXT_ROUTE.toString()).equals("http://mockendservice.com:8082"));

   }

   // TODO Model: fix test
   @Ignore
   @Test
   public void shouldRouteToNextNonLocalHost() throws MalformedURLException {
      nextHostName = "nextHostToRoute";


      routingTagger = new RoutingTagger(interrogator);

      FilterDirector result = routingTagger.handleRequest(request, response);

      final String nextRoute = HostUtilities.asUrl(defaultDest, requestUri);

      assertTrue("Should route to next non-localhost host", result.requestHeaderManager().headersToAdd().get(PowerApiHeader.NEXT_ROUTE.toString().toLowerCase()).contains(nextRoute));

   }
}
