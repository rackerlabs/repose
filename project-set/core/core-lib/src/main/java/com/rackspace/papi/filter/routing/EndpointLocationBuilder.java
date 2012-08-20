package com.rackspace.papi.filter.routing;

import com.rackspace.papi.domain.Port;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.Node;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class EndpointLocationBuilder implements LocationBuilder {

   private final Destination destination;
   private final List<Port> localPorts = new ArrayList<Port>();
   private final Node localhost;
   private final String uri;
   private final HttpServletRequest request;

   public EndpointLocationBuilder(Node localhost, Destination destination, String uri, HttpServletRequest request) {
      this.destination = destination;
      this.localhost = localhost;
      this.uri = uri;
      this.request = request;
      determineLocalPortsList();
   }

   @Override
   public DestinationLocation build() throws MalformedURLException, URISyntaxException {
      return new DestinationLocation(
              new EndpointUrlBuilder(localhost, localPorts, destination, uri, request).build(),
              new EndpointUriBuilder(localPorts, destination, uri, request).build());
   }

   private void determineLocalPortsList() {

      if (localhost.getHttpPort() > 0) {
         localPorts.add(new Port("http", localhost.getHttpPort()));
      }

      if (localhost.getHttpsPort() > 0) {
         localPorts.add(new Port("https", localhost.getHttpsPort()));
      }

   }
}
