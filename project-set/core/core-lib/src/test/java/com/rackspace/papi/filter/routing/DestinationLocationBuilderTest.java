package com.rackspace.papi.filter.routing;

import com.rackspace.papi.model.DestinationCluster;
import com.rackspace.papi.model.DestinationEndpoint;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.service.routing.RoutingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class DestinationLocationBuilderTest {

   public static class WhenConstructingBuilder {
      private RoutingService routingService;
      private Node localhost;
      private DestinationEndpoint endpointDestination;
      private DestinationCluster domainDestination;
      private HttpServletRequest request;

      @Before
      public void setUp() {
         request = mock(HttpServletRequest.class);
         when(request.getScheme()).thenReturn("http");
         when(request.getLocalPort()).thenReturn(8080);
         
         routingService = mock(RoutingService.class);

         localhost = new Node();
         
         localhost.setHttpPort(8080);
         localhost.setHttpsPort(0);
         localhost.setHostname("myhost");
         localhost.setId("local");
         
         endpointDestination = new DestinationEndpoint();
         domainDestination = new DestinationCluster();
      }
      
      @Test
      public void shouldContructAnEnpointBuilder() {
         DestinationLocationBuilder builder = new DestinationLocationBuilder(routingService, localhost, endpointDestination, "", request);
         assertTrue(builder.getBuilder() instanceof EndpointLocationBuilder);
      }

      @Test
      public void shouldContructADomainBuilder() {
         DestinationLocationBuilder builder = new DestinationLocationBuilder(routingService, localhost, domainDestination, "", request);
         assertTrue(builder.getBuilder() instanceof DomainLocationBuilder);
      }
      
      @Test(expected=IllegalArgumentException.class)
      public void shouldThrowIllegalArgumentForNullRouting() {
         new DestinationLocationBuilder(null, localhost, domainDestination, "", request);
      }
      
      @Test(expected=IllegalArgumentException.class)
      public void shouldThrowIllegalArgumentForNullHost() {
         new DestinationLocationBuilder(routingService, null, domainDestination, "", request);
      }
      
      @Test(expected=IllegalArgumentException.class)
      public void shouldThrowIllegalArgumentForNullDestination() {
         new DestinationLocationBuilder(routingService, localhost, null, "", request);
      }
      
   }
}
