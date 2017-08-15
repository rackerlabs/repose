/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.utils.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.UUID;

public final class NetUtilities {
    private static final Logger LOG = LoggerFactory.getLogger(NetUtilities.class);
    private static final NetworkInterfaceProvider NETWORK_INTERFACE_PROVIDER = StaticNetworkInterfaceProvider.getInstance();
    private static final NetworkNameResolver NETWORK_NAME_RESOLVER = StaticNetworkNameResolver.getInstance();
    private static final String DEFAULT_DOMAIN_PREFIX = UUID.randomUUID().toString();

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
        try {
            final InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostAddress();
        } catch (UnknownHostException e) {
            throw new NetUtilitiesException("Failed to get container address", e);

        }
    }

    public static boolean isLocalHost(String hostname) {
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

    /**
     * TODO: Consolidate this function with the getLocalHostName function.
     *
     * Do some logic to figure out what our local hostname is, or get as close as possible
     * references: http://stackoverflow.com/a/7800008/423218 and http://stackoverflow.com/a/17958246/423218
     *
     * @return a string with either the hostname, or something to ID this host
     */
    public static String bestGuessHostname() {
        String result;
        if (System.getProperty("os.name").startsWith("Windows")) {
            LOG.debug("Looking up a windows COMPUTERNAME environment var for the JMX name");
            result = System.getenv("COMPUTERNAME");
        } else {
            LOG.debug("Looking up a linux HOSTNAME environment var for the JMX name");
            //We're probably on linux at this point
            String envHostname = System.getenv("HOSTNAME");
            if (envHostname != null) {
                result = envHostname;
            } else {
                LOG.debug("Unable to find a Linux HOSTNAME environment var, trying another tool");
                //Now we've got to do even more work
                try {
                    result = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    //Weren't able to get the local host :(
                    LOG.warn("Unable to resolve local hostname for JMX", e);
                    result = DEFAULT_DOMAIN_PREFIX;
                }
            }
        }

        return result;
    }

    public static class NetUtilitiesException extends RuntimeException {
        public NetUtilitiesException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
