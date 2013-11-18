package com.rackspace.papi.commons.util.net;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetUtilities {
   public static class NetUtilitiesException extends RuntimeException {
      public NetUtilitiesException(String message, Throwable cause) {
         super(message, cause);
      }
   }
   
   private static final Logger LOG = LoggerFactory.getLogger(NetUtilities.class);
   private static final NetworkInterfaceProvider NETWORK_INTERFACE_PROVIDER = StaticNetworkInterfaceProvider.getInstance();
   private static final NetworkNameResolver NETWORK_NAME_RESOLVER = StaticNetworkNameResolver.getInstance();


   private NetUtilities() {
   }

   public static String getLocalHostName() {
      try {
         final InetAddress addr = InetAddress.getLocalHost();
         return addr.getHostName();
      } catch (UnknownHostException e) {
         throw new NetUtilitiesException("Failed to get hostname. Something weird is going on.", e);
      }
   }
   
   public static String getLocalAddress() {
      try{
         final InetAddress addr = InetAddress.getLocalHost();
         return addr.getHostAddress();
      } catch(UnknownHostException e){
         throw new NetUtilitiesException("Failed to get container address", e);
      
      }
   }
   
   public static boolean isLocalHost(String hostname){
      boolean result = false;

         try {
            final InetAddress hostAddress = NETWORK_NAME_RESOLVER.lookupName(hostname);
            result = NETWORK_INTERFACE_PROVIDER.hasInterfaceFor(hostAddress);
         } catch (UnknownHostException uhe) {
            LOG.error("Unable to look up network host name. Reason: " + uhe.getMessage(), uhe);
         } catch (SocketException socketException) {
            LOG.error(socketException.getMessage(), socketException);
         }

         return result;
   }
}
