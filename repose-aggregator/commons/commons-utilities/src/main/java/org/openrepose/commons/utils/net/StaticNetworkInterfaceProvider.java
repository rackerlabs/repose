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

import org.openrepose.commons.collections.EnumerationIterable;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * @author zinic
 */
public final class StaticNetworkInterfaceProvider implements NetworkInterfaceProvider {

    private static final StaticNetworkInterfaceProvider INSTANCE = new StaticNetworkInterfaceProvider();

    private StaticNetworkInterfaceProvider() {
    }

    public static NetworkInterfaceProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean hasInterfaceFor(InetAddress address) throws SocketException {
        return getInterfaceFor(address) != null;
    }

    @Override
    public NetworkInterface getInterfaceFor(InetAddress address) throws SocketException {
        for (NetworkInterface iface : getNetworkInterfaces()) {
            for (InetAddress ifaceAddress : new EnumerationIterable<>(iface.getInetAddresses())) {
                if (ifaceAddress.equals(address)) {
                    return iface;
                }
            }
        }

        return null;
    }

    @Override
    public Iterable<NetworkInterface> getNetworkInterfaces() throws SocketException {
        return new EnumerationIterable<>(NetworkInterface.getNetworkInterfaces());
    }
}
