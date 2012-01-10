package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.net.StaticNetworkNameResolver;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.commons.util.net.NetworkInterfaceProvider;
import com.rackspace.papi.commons.util.net.NetworkNameResolver;
import com.rackspace.papi.commons.util.net.StaticNetworkInterfaceProvider;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
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
   private final int localPort;

   public SystemModelInterrogator(PowerProxy powerProxy, int port) {
      this(StaticNetworkNameResolver.getInstance(), StaticNetworkInterfaceProvider.getInstance(), powerProxy, port);
   }

   public SystemModelInterrogator(NetworkNameResolver nameResolver, NetworkInterfaceProvider nip, PowerProxy systemModel, int port) {
      this.nameResolver = nameResolver;
      this.networkInterfaceProvider = nip;
      this.systemModel = systemModel;
      this.localPort = port;
   }

   public Host getLocalHost() {
      Host localHost = null;

      final List<Host> possibleHosts = new LinkedList<Host>();

      for (Host powerProxyHost : systemModel.getHost()) {
         if (powerProxyHost.getServicePort() == localPort) {
            possibleHosts.add(powerProxyHost);
         }
      }

      try {
         for (Host powerProxyHost : possibleHosts) {
            final InetAddress hostAddress = nameResolver.lookupName(powerProxyHost.getHostname());
            
            if (networkInterfaceProvider.hasInterfaceFor(hostAddress)) {
               localHost = powerProxyHost;
               break;
            }
         }
      } catch (UnknownHostException uhe) {
         LOG.error("Unable to look up network host name. Reason: " + uhe.getMessage(), uhe);
      } catch (SocketException socketException) {
         LOG.error(socketException.getMessage(), socketException);
      }

      return localHost;
   }

   // TODO: Enhancement - Explore using service domains to better handle routing identification logic
   public Host getNextRoutableHost() {
      final Host localHost = getLocalHost();

      Host nextRoutableHost = null;

      for (Iterator<Host> hostIterator = systemModel.getHost().iterator(); hostIterator.hasNext();) {
         final Host currentHost = hostIterator.next();

         if (currentHost.equals(localHost)) {
            nextRoutableHost = hostIterator.hasNext() ? hostIterator.next() : null;
            break;
         }
      }

      return nextRoutableHost;
   }
}