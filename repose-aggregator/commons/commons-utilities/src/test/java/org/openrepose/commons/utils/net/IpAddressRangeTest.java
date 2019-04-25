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

import org.junit.Before;
import org.junit.Test;

import java.net.UnknownHostException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IpAddressRangeTest {

    private final String cidr1 = "198.51.100.0/22"; // should include addresses from 198.51.100.0 to 198.51.103.255
    private final String address = "192.168.1.1";
    private final String cidrIpV6 = "2001:db8::/48";
    private IpAddressRange range1;
    private IpAddressRange range2;
    private IpAddressRange range3;
    private IpAddressRange range4;

    @Before
    public void setUp() throws UnknownHostException {
        range1 = new IpAddressRange(cidr1);
        range2 = new IpAddressRange(address);
        range3 = new IpAddressRange(cidrIpV6);
        range4 = new IpAddressRange(address, 48);
    }

    @Test
    public void shouldFindAddressesInRange() throws UnknownHostException {
        assertTrue(range1.addressInRange("198.51.100.254"));
    }

    /**
     * This is kind of a bad test. If the network is doing odd things, like auto resolving things for you
     * I found this failure when I was working remotely, the network I'm on happily translates "invalid" into an
     * IP address, and this test then fails, and I cannot continue the compilation
     * <p/>
     * TODO: BRITTLE TEST
     *
     * @throws UnknownHostException
     */
    @Test(expected = UnknownHostException.class)
    public void shouldThrowExceptionForInvalidAddress() throws UnknownHostException {
        range1.addressInRange("Invalid");
    }

    @Test
    public void shouldNotFindAddressesThatAreNotInRange() throws UnknownHostException {
        assertFalse(range1.addressInRange("198.51.104.1"));
    }

    @Test
    public void shouldHandleExactAddresses() throws UnknownHostException {
        assertTrue(range2.addressInRange(address));
        assertFalse(range2.addressInRange("192.168.1.2"));
    }

    @Test
    public void shouldHandleIpV6() throws UnknownHostException {
        assertTrue(range3.addressInRange("2001:db8::1"));
        assertFalse(range3.addressInRange("2001:db9::1"));
        assertFalse(range3.addressInRange("127.0.0.1"));
    }

    @Test
    public void shouldHandleInvalidMask() throws UnknownHostException {
        assertTrue(range4.addressInRange(address));
        assertFalse(range4.addressInRange("192.168.1.2"));
    }

}
