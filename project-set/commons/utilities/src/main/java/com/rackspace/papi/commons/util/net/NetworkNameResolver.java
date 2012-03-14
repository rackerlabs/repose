package com.rackspace.papi.commons.util.net;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author zinic
 */
public interface NetworkNameResolver {

   InetAddress lookupName(String host) throws UnknownHostException;
   
}
