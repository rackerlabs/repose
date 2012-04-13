package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DestinationDomain;
import com.rackspace.papi.model.DestinationEndpoint;
import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.service.routing.RoutingService;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class DestinationUriBuilder {

   private final static String HTTP_PROTOCOL = "http";
   private final DomainNode localhost;
   private final Destination destination;
   private final RoutingService routingService;
   private final UriBuilder builder;
   private final List<Port> localPorts = new ArrayList<Port>();
   private final String uri;

   private static interface UriBuilder {

      public URI build() throws URISyntaxException;
   }
   
   private class DomainUrlBuilder implements UriBuilder {

      private final DestinationDomain domain;

      private DomainUrlBuilder() {
         this.domain = (DestinationDomain) destination;
      }

      @Override
      public URI build() throws URISyntaxException {
         DomainNode node = routingService.getRoutableNode(domain.getId());
         int port = HTTP_PROTOCOL.equalsIgnoreCase(domain.getProtocol())? node.getHttpPort(): node.getHttpsPort();
         return new URI(domain.getProtocol(), null, node.getHostname(), port, domain.getRootPath() + uri, null, null);
      }
   }

   private class EndpointUrlBuilder implements UriBuilder {

      private final DestinationEndpoint endpoint;

      private String determineScheme() {
         String scheme = destination.getProtocol();
         if (StringUtilities.isBlank(scheme) || endpoint.getPort() <= 0) {
            // no scheme or port specified means this is an internal dispatch
            return null;
         }
         return scheme;
      }

      private String determineHostname(String scheme) {
         if (StringUtilities.isBlank(scheme)) {
            return null;
         }

         Port port = new Port(scheme, endpoint.getPort());

         if (endpoint.getHostname() == null || "localhost".equalsIgnoreCase(endpoint.getHostname())) {
            if (localPorts.contains(port)) {
               // internal dispatch
               return null;
            }

            // dispatching to this host, but not our port
            return localhost.getHostname();
         }

         return endpoint.getHostname();
      }

      EndpointUrlBuilder() {
         endpoint = (DestinationEndpoint) destination;

      }

      @Override
      public URI build() throws URISyntaxException {
         String scheme = determineScheme();
         String hostname = determineHostname(scheme);
         String rootPath = endpoint.getRootPath();
         StringBuilder path = new StringBuilder(rootPath);
         
         if (!rootPath.isEmpty() && !uri.isEmpty()) {
            if (!rootPath.endsWith("/") && !uri.startsWith("/")) {
               path.append("/");
               path.append(uri);
            } else if (rootPath.endsWith("/") && uri.startsWith("/")) {
               path.append(uri.substring(1));
            } else {
               path.append(uri);
            }
         } else if (!uri.isEmpty()) {
            if (!uri.startsWith("/")) {
               path.append("/");
            }
            path.append(uri);
         }
         int port = scheme == null || hostname == null ? -1 : endpoint.getPort();

         return new URI(hostname != null? scheme: null, null, hostname, port, path.toString(), null, null);
      }
   }

   private void determineLocalPortsList() {

      if (localhost.getHttpPort() > 0) {
         localPorts.add(new Port("http", localhost.getHttpPort()));
      }

      if (localhost.getHttpsPort() > 0) {
         localPorts.add(new Port("https", localhost.getHttpsPort()));
      }

   }

   public DestinationUriBuilder(RoutingService routingService, DomainNode localhost, Destination destination, String uri) {
      if (localhost == null) {
         throw new IllegalArgumentException("localhost cannot be null");
      }
      if (destination == null) {
         throw new IllegalArgumentException("destination cannot be null");
      }
      if (routingService == null) {
         throw new IllegalArgumentException("routingService cannot be null");
      }

      this.localhost = localhost;
      this.destination = destination;
      this.routingService = routingService;
      this.uri = StringUtilities.getNonBlankValue(uri, "");
      determineLocalPortsList();

      if (destination instanceof DestinationEndpoint) {
         builder = new EndpointUrlBuilder();
      } else if (destination instanceof DestinationDomain) {
         builder = new DomainUrlBuilder();
      } else {
         throw new IllegalArgumentException("Unknown destination type: " + destination.getClass().getName());
      }
   }

   public URI build() throws URISyntaxException {
      return builder.build();
   }
}
