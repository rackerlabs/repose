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
import com.rackspace.papi.model.Host;
import com.rackspace.papi.commons.util.net.NetUtilities;
import com.rackspace.papi.commons.util.net.NetworkInterfaceProvider;
import com.rackspace.papi.commons.util.net.NetworkNameResolver;
import com.rackspace.papi.filter.SystemModelInterrogator;


import com.rackspace.papi.model.FilterList;
import java.net.InetAddress;
import org.junit.Before;
import org.junit.Test;
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
   PowerProxy systemModel;
   String myHostName, requestUri, nextHostName;

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

      interrogator = new SystemModelInterrogator(resolver, interfaceProvider, systemModel, 8080);

      Host localHost = new Host();
      localHost.setHostname("localhost");
      localHost.setFilters(mock(FilterList.class));
      localHost.setServicePort(8080);
      
      systemModel.getHost().add(localHost);
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

   @Test
   public void shouldRouteToNextNonLocalHost() throws MalformedURLException {
      nextHostName = "nextHostToRoute";

      final Host nextHost = new Host();
      nextHost.setHostname(nextHostName);
      nextHost.setFilters(mock(FilterList.class));
      nextHost.setServicePort(8081);

      systemModel.getHost().add(nextHost);
      routingTagger = new RoutingTagger(interrogator);

      FilterDirector result = routingTagger.handleRequest(request, response);

      final String nextRoute = HostUtilities.asUrl(nextHost, requestUri);

      assertTrue("Should route to next non-localhost host", result.requestHeaderManager().headersToAdd().get(PowerApiHeader.NEXT_ROUTE.toString().toLowerCase()).contains(nextRoute));

   }
}
