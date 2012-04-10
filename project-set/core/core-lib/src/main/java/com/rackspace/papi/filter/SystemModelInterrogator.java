package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.net.StaticNetworkNameResolver;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.commons.util.net.NetworkInterfaceProvider;
import com.rackspace.papi.commons.util.net.NetworkNameResolver;
import com.rackspace.papi.commons.util.net.StaticNetworkInterfaceProvider;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.model.ServiceDomain;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
   private final List<Port> ports;

   public SystemModelInterrogator(PowerProxy powerProxy, List<Port> ports) {
      this(StaticNetworkNameResolver.getInstance(), StaticNetworkInterfaceProvider.getInstance(), powerProxy, ports);
   }

   public SystemModelInterrogator(NetworkNameResolver nameResolver, NetworkInterfaceProvider nip, PowerProxy systemModel, List<Port> ports) {
      this.nameResolver = nameResolver;
      this.networkInterfaceProvider = nip;
      this.systemModel = systemModel;
      this.ports = ports;
   }

   public class DomainNodeWrapper {

      private final DomainNode node;

      private DomainNodeWrapper(DomainNode node) {
         if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
         }
         this.node = node;
      }

      public boolean hasLocalInterface() {
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

      private List<Port> getPortsList() {
         List<Port> ports = new ArrayList<Port>();
         
         
         // TODO Model: use constants or enum for possible protocols
         if (node.getHttpPort() > 0) {
            ports.add(new Port("http", node.getHttpPort()));
         }
         
         if (node.getHttpsPort() > 0) {
            ports.add(new Port("https", node.getHttpsPort()));
         }
         
         return ports;
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

      public boolean containsLocalNodeForPorts(List<Port> ports) {
         return getLocalNodeForPorts(ports) != null;
      }
      
      public DomainNode getLocalNodeForPorts(List<Port> ports) {

         DomainNode localhost = null;
         
         if (ports.isEmpty()) {
            return localhost;
         }

         for (DomainNode host : domain.getServiceDomainNodes().getNode()) {
            DomainNodeWrapper wrapper = new DomainNodeWrapper(host);
            List<Port> hostPorts = wrapper.getPortsList();
            
            if (hostPorts.equals(ports) && wrapper.hasLocalInterface()) {
               localhost = host;
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
         if (new ServiceDomainWrapper(possibleDomain).containsLocalNodeForPorts(ports)) {
            domain = possibleDomain;
            break;
         }
      }

      return domain;
   }

   public DomainNode getLocalHost() {
      DomainNode localHost = null;

      for (ServiceDomain domain : systemModel.getServiceDomain()) {
         DomainNode node = new ServiceDomainWrapper(domain).getLocalNodeForPorts(ports);

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