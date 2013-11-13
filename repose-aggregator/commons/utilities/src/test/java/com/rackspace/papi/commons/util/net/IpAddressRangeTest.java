package com.rackspace.papi.commons.util.net;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.net.UnknownHostException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class IpAddressRangeTest {

   public static class WhenCheckingForAddressesInRange {
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
      
      @Test(expected=UnknownHostException.class)
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

}
