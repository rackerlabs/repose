package com.rackspace.papi.commons.util.net;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class NetUtilities {
   public static class NetUtilitiesException extends RuntimeException {
      public NetUtilitiesException(String message, Throwable cause) {
         super(message, cause);
      }
   }


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
}
