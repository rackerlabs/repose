package com.rackspace.papi.filter.routing;

import com.rackspace.papi.commons.util.StringUriUtilities;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DestinationEndpoint;
import com.rackspace.papi.model.Node;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class EndpointUrlBuilder {

   private final DestinationEndpoint endpoint;
   private final Node localhost;
   private final String uri;
   private final List<Port> localPorts;
   private final HttpServletRequest request;

   EndpointUrlBuilder(Node localhost, List<Port> localPorts, Destination destination, String uri, HttpServletRequest request) {
      this.localhost = localhost;
      this.uri = uri;
      this.localPorts = localPorts;
      this.request = request;
      endpoint = (DestinationEndpoint) destination;

   }

   private int localPortForProtocol(String protocol) {
      for (Port port : localPorts) {
         if (port.getProtocol().equalsIgnoreCase(protocol)) {
            return port.getPort();
         }
      }

      return 0;
   }

   private Port determineUrlPort() throws MalformedURLException {
      if (!StringUtilities.isBlank(endpoint.getProtocol())) {
         int port = endpoint.getPort() <= 0 ? localPortForProtocol(endpoint.getProtocol()) : endpoint.getPort();
         return new Port(endpoint.getProtocol(), port);
      }

      Port port = new Port(request.getScheme(), request.getLocalPort());
      if (localPorts.contains(port)) {
         return port;
      }

      throw new MalformedURLException("Cannot determine destination port.");
   }

   private String determineHostname() {
      String hostname = endpoint.getHostname();

      if (StringUtilities.isBlank(hostname)) {
         // endpoint is local
         hostname = localhost.getHostname();
      }

      return hostname;
   }

   public URL build() throws MalformedURLException {
      Port port = determineUrlPort();
      String hostname = determineHostname();
      String rootPath = endpoint.getRootPath();
      String path = StringUriUtilities.concatUris(rootPath, uri);

      return new URL(port.getProtocol(), hostname, port.getPort(), path);
   }
}
