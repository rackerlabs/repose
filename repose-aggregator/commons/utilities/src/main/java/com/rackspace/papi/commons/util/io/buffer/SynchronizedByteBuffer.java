package com.rackspace.papi.commons.util.io.buffer;

import java.io.IOException;

public class SynchronizedByteBuffer implements ByteBuffer {

   private final ByteBuffer internalBuffer;

   public SynchronizedByteBuffer(ByteBuffer internalBuffer) {
      this.internalBuffer = internalBuffer;
   }

   @Override
   public synchronized void clear() {
      internalBuffer.clear();
   }

   @Override
   public synchronized int skip(int bytes) {
      return internalBuffer.skip(bytes);
   }

   @Override
   public synchronized int remaining() {
      return internalBuffer.remaining();
   }

   @Override
   public synchronized int put(byte[] b, int off, int len) throws IOException {
      return internalBuffer.put(b, off, len);
   }

   @Override
   public synchronized int put(byte[] b) throws IOException {
      return internalBuffer.put(b);
   }

   @Override
   public synchronized void put(byte b) throws IOException {
      internalBuffer.put(b);
   }

   @Override
   public synchronized int get(byte[] b, int off, int len) throws IOException {
      return internalBuffer.get(b, off, len);
   }

   @Override
   public synchronized int get(byte[] b) throws IOException {
      return internalBuffer.get(b);
   }

   @Override
   public synchronized byte get() throws IOException {
      return internalBuffer.get();
   }

   @Override
   public synchronized ByteBuffer copy() {
      return internalBuffer.copy();
   }

   @Override
   public synchronized int available() {
      return internalBuffer.available();
   }
}
