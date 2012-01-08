package com.rackspace.papi.filter;

import com.rackspace.papi.model.Host;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.commons.util.net.NetUtilities;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
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
   private final PowerProxy systemModel;
   private final int port;

   public SystemModelInterrogator(PowerProxy powerProxy, int port) {
      this.systemModel = powerProxy;
      this.port = port;
   }

   public Host getLocalHost() {
      Host localHost = null;
      
      final List<Host> possibleHosts = new LinkedList<Host>();
      
      for (Host powerProxyHost : systemModel.getHost()) {
         if (powerProxyHost.getServicePort() == port) {
            possibleHosts.add(powerProxyHost);
         }
      }

      try {
         final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

         while (localHost == null && interfaces.hasMoreElements()) {
            final NetworkInterface networkInterface = interfaces.nextElement();
            
            localHost = findHost(possibleHosts, networkInterface.getInetAddresses());
         }
      } catch (SocketException socketException) {
         LOG.error(socketException.getMessage(), socketException);
      }

      return localHost;
   }

   private Host findHost(List<Host> hosts, Enumeration<InetAddress> interfaceAddresses) {
      try {
         while (interfaceAddresses.hasMoreElements()) {
            final InetAddress interfaceAddress = interfaceAddresses.nextElement();

            for (Host powerProxyHost : hosts) {
               final InetAddress hostAddress = InetAddress.getByName(powerProxyHost.getHostname());

               if (hostAddress.equals(interfaceAddress)) {
                  return powerProxyHost;
               }
            }
         }
      } catch (UnknownHostException uhe) {
         LOG.error(uhe.getMessage(), uhe);
      }

      return null;
   }

   // TODO: Enhancement - Explore using service domains to better handle routing identification logic
   public Host getNextRoutableHost() {
      final String myHostname = NetUtilities.getLocalHostName();
      final List<Host> hosts = systemModel.getHost();
      Host nextRoutableHost = null;

      for (Iterator<Host> hostIterator = hosts.iterator(); hostIterator.hasNext();) {
         final Host currentHost = hostIterator.next();

         if (currentHost.getHostname().equals(myHostname)) {
            nextRoutableHost = hostIterator.hasNext() ? hostIterator.next() : null;
            break;
         }
      }

      return nextRoutableHost;
   }
   // Moved getLocalHostName method to com.rackspace.papi.commons.util.net.NetUtilities;
}