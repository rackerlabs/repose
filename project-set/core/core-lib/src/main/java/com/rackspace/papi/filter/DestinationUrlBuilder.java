package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DestinationDomain;
import com.rackspace.papi.model.DestinationEndpoint;
import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.service.routing.RoutingService;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class DestinationUrlBuilder {

   private final static String HTTP_PROTOCOL = "http";
   private final DomainNode localhost;
   private final Destination destination;
   private final RoutingService routingService;
   private final UrlBuilder builder;
   private final List<Port> localPorts = new ArrayList<Port>();
   private final String uri;
   private final HttpServletRequest request;

   private static interface UrlBuilder {

      public URL build() throws MalformedURLException;
   }

   private class DomainUrlBuilder implements UrlBuilder {

      private final DestinationDomain domain;

      private DomainUrlBuilder() {
         this.domain = (DestinationDomain) destination;
      }

      @Override
      public URL build() throws MalformedURLException {
         DomainNode node = routingService.getRoutableNode(domain.getId());
         int port = HTTP_PROTOCOL.equalsIgnoreCase(domain.getProtocol()) ? node.getHttpPort() : node.getHttpsPort();
         return new URL(domain.getProtocol(), node.getHostname(), port, domain.getRootPath() + uri);
      }
   }

   private class EndpointUrlBuilder implements UrlBuilder {

      private final DestinationEndpoint endpoint;

      private Port determinePort() throws MalformedURLException {
         if (!StringUtilities.isBlank(endpoint.getProtocol())) {
            return new Port(endpoint.getProtocol(), endpoint.getPort());
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

      EndpointUrlBuilder() {
         endpoint = (DestinationEndpoint) destination;

      }

      @Override
      public URL build() throws MalformedURLException {
         Port port = determinePort();
         String hostname = determineHostname();
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

         return new URL(port.getProtocol(), hostname, port.getPort(), path.toString());
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

   public DestinationUrlBuilder(RoutingService routingService, DomainNode localhost, Destination destination, String uri, HttpServletRequest request) {
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
      this.request = request;
      determineLocalPortsList();

      if (destination instanceof DestinationEndpoint) {
         builder = new EndpointUrlBuilder();
      } else if (destination instanceof DestinationDomain) {
         builder = new DomainUrlBuilder();
      } else {
         throw new IllegalArgumentException("Unknown destination type: " + destination.getClass().getName());
      }
   }

   public URL build() throws MalformedURLException {
      return builder.build();
   }
}
