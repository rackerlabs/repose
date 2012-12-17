package com.rackspace.papi.commons.util.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpAddressRange {

   private static final Logger LOG = LoggerFactory.getLogger(IpAddressRange.class);
   private static final int BYTE_SIZE = 8;
   private final byte[] network;
   private final int mask;

   public IpAddressRange(String cidr) throws UnknownHostException {
      String[] parts = cidr.split("/");
      InetAddress[] addresses = InetAddress.getAllByName(parts[0]);
      if (addresses.length == 0) {
         throw new IllegalArgumentException("Unable to determine addresses for cidr: " + cidr);
      }
      
      if (addresses.length > 1) {
         LOG.warn("Multiple addresses found for cidr " + cidr + " Using first available.");
      }
      
      InetAddress address = addresses[0];
      network = address.getAddress();
      if (parts.length > 1) {
         mask = Integer.valueOf(parts[1]);
      } else {
         mask = network.length * BYTE_SIZE;
      }

   }

   public IpAddressRange(String network, int mask) throws UnknownHostException {
      this.network = InetAddress.getByName(network).getAddress();
      this.mask = mask;
   }

   public boolean addressInRange(String address) throws UnknownHostException {
      byte[] target = InetAddress.getByName(address).getAddress();
      return match(getIp(), target, getMask());
   }

   public boolean addressInRange(byte[] address) throws UnknownHostException {
      return match(getIp(), address, getMask());
   }

   /**
    * Compare "bits" bits of two byte arrays begining with the left most bits.
    *
    * @param array1
    * @param array2
    * @param bits
    * @return true if the left most "bits" compare
    */
   private boolean match(byte[] array1, byte[] array2, int bits) {
      boolean match = array1.length == array2.length;
      int bitRemaining = bits;

      int index = 0;
      while (match && bitRemaining > 0 && index < array1.length) {
         match &= match(array1[index], array2[index], bitRemaining > BYTE_SIZE ? BYTE_SIZE : bitRemaining);
         bitRemaining -= BYTE_SIZE;
         index++;
      }

      return match;
   }

   /**
    * Match the left most bits of two byte values
    *
    * @param byte1
    * @param byte2
    * @param bits
    * @return true if the first "bits" bit values of byte1 and byte2 match.
    */
   private boolean match(byte byte1, byte byte2, int bits) {
      int shift = (BYTE_SIZE - bits);

      int first = (byte1 >> shift) << shift;
      int second = (byte2 >> shift) << shift;

      return (first ^ second) == 0;
   }

   public byte[] getIp() {
      return (byte[])network.clone();
   }

   public int getMask() {
      return mask;
   }
}
