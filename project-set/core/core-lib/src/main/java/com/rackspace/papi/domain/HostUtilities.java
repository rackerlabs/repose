package com.rackspace.papi.domain;

import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DestinationDomain;
import com.rackspace.papi.model.DestinationEndpoint;
import com.rackspace.papi.model.DomainNode;
import java.net.MalformedURLException;
import java.net.URL;

public final class HostUtilities {

   private HostUtilities() {
   }

   public static String asUrl(Destination target) throws MalformedURLException {
      return asUrl(target, "");
   }
   
   private static int getPortForProtocol(DomainNode node, String protocol) {
      if ("http".equalsIgnoreCase(protocol)) {
         return node.getHttpPort();
      } else if ("https".equalsIgnoreCase(protocol)) {
         return node.getHttpsPort();
      }
      
      return 0;
   }

   public static String asUrl(Destination target, String uri) throws MalformedURLException {
      if (target instanceof DestinationEndpoint) {
         DestinationEndpoint endpoint = (DestinationEndpoint)target;
         return new URL(endpoint.getProtocol(), endpoint.getHostname(), endpoint.getPort(), uri).toExternalForm();

      } else if (target instanceof DestinationDomain) {
         // TODO Model: route to a host within the domain
         DestinationDomain domain = (DestinationDomain) target;
         
         // For now, just grab the first node and route to that
         if (!domain.getTargetDomain().getServiceDomainNodes().getNode().isEmpty()) {
            DomainNode node = domain.getTargetDomain().getServiceDomainNodes().getNode().get(0);
            int port = getPortForProtocol(node, target.getProtocol());

            if (port > 0) {
               return new URL(target.getProtocol(), node.getHostname(), port, uri).toExternalForm();
            }
         }
      }
      return null;
   }
}
