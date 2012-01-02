/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.commons.util.net;

import java.net.InetAddress;
import java.net.UnknownHostException;



public final class NetUtilities {

    private NetUtilities() {
    }
    
    
    public static String getLocalHostName() {
        try {
            final InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Failed to get hostname. Something weird is going on.", e);
        }
    }
    
}
