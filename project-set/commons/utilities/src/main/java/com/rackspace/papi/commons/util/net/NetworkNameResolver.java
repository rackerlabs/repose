/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
