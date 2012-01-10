package com.rackspace.papi.commons.util.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 *
 * @author zinic
 */
public interface NetworkInterfaceProvider {
   
   boolean hasInterfaceFor(InetAddress address) throws SocketException;
   
   NetworkInterface getInterfaceFor(InetAddress address) throws SocketException;

   Iterable<NetworkInterface> getNetworkInterfaces() throws SocketException;
}
