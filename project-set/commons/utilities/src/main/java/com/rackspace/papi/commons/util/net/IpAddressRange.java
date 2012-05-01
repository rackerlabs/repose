package com.rackspace.papi.commons.util.net;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpAddressRange {

   private final byte[] network;
   private final int mask;

   public IpAddressRange(String cidr) throws UnknownHostException {
      String[] parts = cidr.split("/");
      network = InetAddress.getByName(parts[0]).getAddress();
      if (parts.length > 1) {
         mask = Integer.valueOf(parts[1]);
      } else {
         mask = network.length * 8;
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

      int index = 0;
      while (match && bits > 0 && index < array1.length) {
         match &= match(array1[index], array2[index], bits > 8 ? 8 : bits);
         bits -= 8;
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
      int shift = (8 - bits);

      int first = (byte1 >> shift) << shift;
      int second = (byte2 >> shift) << shift;

      return (first ^ second) == 0;
   }

   public byte[] getIp() {
      return network;
   }

   public int getMask() {
      return mask;
   }
}
