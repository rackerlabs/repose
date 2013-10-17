package com.rackspace.papi.commons.util.net;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author zinic
 */
public final class StaticNetworkNameResolver implements NetworkNameResolver {

   private static final StaticNetworkNameResolver INSTANCE = new StaticNetworkNameResolver();
   
   public static StaticNetworkNameResolver getInstance() {
      return INSTANCE;
   }
   
   private StaticNetworkNameResolver() {
      
   }
   
   @Override
   public InetAddress lookupName(String host) throws UnknownHostException {
      return InetAddress.getByName(host);
   }
}
