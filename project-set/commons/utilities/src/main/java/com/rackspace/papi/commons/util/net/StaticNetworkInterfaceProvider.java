package com.rackspace.papi.commons.util.net;

import com.rackspace.papi.commons.collections.EnumerationIterable;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 *
 * @author zinic
 */
public final class StaticNetworkInterfaceProvider implements NetworkInterfaceProvider {

   private static final StaticNetworkInterfaceProvider INSTANCE = new StaticNetworkInterfaceProvider();

   public static NetworkInterfaceProvider getInstance() {
      return INSTANCE;
   }

   private StaticNetworkInterfaceProvider() {
   }

   @Override
   public boolean hasInterfaceFor(InetAddress address) throws SocketException {
      return getInterfaceFor(address) != null;
   }

   @Override
   public NetworkInterface getInterfaceFor(InetAddress address) throws SocketException {
      for (NetworkInterface iface : getNetworkInterfaces()) {
         for (InetAddress ifaceAddress : new EnumerationIterable<InetAddress>(iface.getInetAddresses())) {
            if (ifaceAddress.equals(address)) {
               return iface;
            }
         }
      }

      return null;
   }

   @Override
   public Iterable<NetworkInterface> getNetworkInterfaces() throws SocketException {
      return new EnumerationIterable<NetworkInterface>(NetworkInterface.getNetworkInterfaces());
   }
}
