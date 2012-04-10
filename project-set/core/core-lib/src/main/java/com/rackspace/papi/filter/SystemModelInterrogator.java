package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.net.StaticNetworkNameResolver;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.commons.util.net.NetworkInterfaceProvider;
import com.rackspace.papi.commons.util.net.NetworkNameResolver;
import com.rackspace.papi.commons.util.net.StaticNetworkInterfaceProvider;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.model.ServiceDomain;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author franshua
 */
public class SystemModelInterrogator {

   private static final Logger LOG = LoggerFactory.getLogger(SystemModelInterrogator.class);
   private final NetworkInterfaceProvider networkInterfaceProvider;
   private final NetworkNameResolver nameResolver;
   private final PowerProxy systemModel;
   private final int httpPort;
   private final int httpsPort;

   public SystemModelInterrogator(PowerProxy powerProxy, int httpPort, int httpsPort) {
      this(StaticNetworkNameResolver.getInstance(), StaticNetworkInterfaceProvider.getInstance(), powerProxy, httpPort, httpsPort);
   }

   public SystemModelInterrogator(NetworkNameResolver nameResolver, NetworkInterfaceProvider nip, PowerProxy systemModel, int httpPort, int httpsPort) {
      this.nameResolver = nameResolver;
      this.networkInterfaceProvider = nip;
      this.systemModel = systemModel;
      this.httpPort = httpPort;
      this.httpsPort = httpsPort;
   }

   public class DomainNodeWrapper {

      private final DomainNode node;

      private DomainNodeWrapper(DomainNode node) {
         if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
         }
         this.node = node;
      }

      public boolean hasLocalInterfaceForPorts(int httpPort, int httpsPorts) {
         boolean result = false;

         try {
            final InetAddress hostAddress = nameResolver.lookupName(node.getHostname());
            result = networkInterfaceProvider.hasInterfaceFor(hostAddress);
         } catch (UnknownHostException uhe) {
            LOG.error("Unable to look up network host name. Reason: " + uhe.getMessage(), uhe);
         } catch (SocketException socketException) {
            LOG.error(socketException.getMessage(), socketException);
         }

         return result;
      }
   }

   public class ServiceDomainWrapper {

      private final ServiceDomain domain;

      private ServiceDomainWrapper(ServiceDomain domain) {
         if (domain == null) {
            throw new IllegalArgumentException("Domain cannot be null");
         }
         this.domain = domain;
      }

      public boolean containsLocalNodeForPorts(int httpPort, int httpsPort) {
         return getLocalNodeForPorts(httpPort, httpsPort) != null;
      }

      public DomainNode getLocalNodeForPorts(int httpPort, int httpsPort) {

         final List<DomainNode> possibleHosts = new LinkedList<DomainNode>();
         DomainNode localhost = null;

         for (DomainNode host : domain.getServiceDomainNodes().getNode()) {
            if ((host.getHttpPort() == httpPort || httpPort <= 0)
                    && (host.getHttpsPort() == httpsPort || httpsPort <= 0)) {

               // If localPort <= 0 then this is the case where we are running ROOT.war so we
               // don't have easy programmatic access to the port on which
               // the local Repose is running.  So, just use the hostname to
               // resolve which is the local Repose node.
               possibleHosts.add(host);
            }
         }

         for (DomainNode powerProxyHost : possibleHosts) {
            DomainNodeWrapper wrapper = new DomainNodeWrapper(powerProxyHost);

            if (wrapper.hasLocalInterfaceForPorts(httpPort, httpsPort)) {
               localhost = powerProxyHost;
               break;
            }

         }

         return localhost;
      }

      public Destination getDefaultDestination() {
         Destination dest = null;

         List<Destination> destinations = new ArrayList<Destination>();

         destinations.addAll(domain.getDestinations().getEndpoint());
         destinations.addAll(domain.getDestinations().getTargetDomain());

         for (Destination destination : destinations) {
            if (destination.isDefault()) {
               dest = destination;
               break;
            }
         }

         return dest;
      }
   }

   public ServiceDomain getLocalServiceDomain() {
      ServiceDomain domain = null;

      for (ServiceDomain possibleDomain : systemModel.getServiceDomain()) {
         if (new ServiceDomainWrapper(possibleDomain).containsLocalNodeForPorts(httpPort, httpsPort)) {
            domain = possibleDomain;
            break;
         }
      }

      return domain;
   }

   public DomainNode getLocalHost() {
      DomainNode localHost = null;

      for (ServiceDomain domain : systemModel.getServiceDomain()) {
         DomainNode node = new ServiceDomainWrapper(domain).getLocalNodeForPorts(httpPort, httpsPort);

         if (node != null) {
            localHost = node;
            break;
         }
      }

      return localHost;
   }

   public Destination getDefaultDestination() {
      ServiceDomainWrapper domain = new ServiceDomainWrapper(getLocalServiceDomain());

      return domain.getDefaultDestination();
   }
}