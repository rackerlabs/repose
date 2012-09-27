package com.rackspace.papi.commons.util.io.buffer;

/**
 *
 * @author zinic
 */
public final class HeapspaceByteArrayProvider implements ByteArrayProvider {

   private static final HeapspaceByteArrayProvider INSTANCE = new HeapspaceByteArrayProvider();

   public static ByteArrayProvider getInstance() {
      return INSTANCE;
   }

   private HeapspaceByteArrayProvider() {
   }

   @Override
   public byte[] allocate(int capacity) {
      if (capacity <= 0) {
         throw new IllegalArgumentException("Byte array capacity must be greater than zero - got " + capacity);
      }

      return new byte[capacity];
   }
}
