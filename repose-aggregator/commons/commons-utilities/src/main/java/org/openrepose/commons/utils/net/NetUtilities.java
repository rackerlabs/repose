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

public final class NetUtilities {
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

    public static class NetUtilitiesException extends RuntimeException {
        public NetUtilitiesException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
