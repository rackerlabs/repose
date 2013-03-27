package com.rackspace.papi.commons.util.io.buffer;

import java.io.IOException;

public class CyclicByteBuffer implements ByteBuffer, Cloneable {

   private static final ByteArrayProvider DEFAULT_BYTE_ARRAY_PROVIDER = HeapspaceByteArrayProvider.getInstance();
   private static final int DEFAULT_BUFFER_SIZE = 2048; 
   private final int initialSize;
   private final ByteArrayProvider byteArrayProvider;
   private int nextWritableIndex, nextReadableIndex;
   private boolean hasElements;
   private byte[] buffer;

   public CyclicByteBuffer() {
      this(DEFAULT_BYTE_ARRAY_PROVIDER, DEFAULT_BUFFER_SIZE, 0, 0, false, false);
   }

   public CyclicByteBuffer(int initialSize, boolean lazyAllocate) {
      this(DEFAULT_BYTE_ARRAY_PROVIDER, initialSize, 0, 0, false, lazyAllocate);
   }

   public CyclicByteBuffer(ByteArrayProvider byteArrayProvider) {
      this(byteArrayProvider, DEFAULT_BUFFER_SIZE, 0, 0, false, false);
   }

   protected CyclicByteBuffer(ByteArrayProvider byteArrayProvider, int allocationSize, int nextWritableIndex, int nextReadableIndex, boolean hasElements, boolean lazyAllocate) {
      this.byteArrayProvider = byteArrayProvider;
      this.nextWritableIndex = nextWritableIndex;
      this.nextReadableIndex = nextReadableIndex;
      this.hasElements = hasElements;
      this.initialSize = allocationSize;
      if (lazyAllocate) {
         buffer = null;
      } else {
         buffer = byteArrayProvider.allocate(initialSize);
      }
   }

   public CyclicByteBuffer(ByteArrayProvider byteArrayProvider, CyclicByteBuffer byteBuffer) {
      this.byteArrayProvider = byteArrayProvider;
      final int readableLength = byteBuffer.available();
      final int allocationSize = (readableLength > 0 && readableLength > DEFAULT_BUFFER_SIZE ? readableLength : DEFAULT_BUFFER_SIZE);
      initialSize = allocationSize;

      if (byteBuffer.buffer != null) {

         buffer = byteArrayProvider.allocate(allocationSize);

         if (byteBuffer.nextReadableIndex + readableLength > byteBuffer.buffer.length) {
            final int trimmedLength = byteBuffer.buffer.length - byteBuffer.nextReadableIndex;

            System.arraycopy(byteBuffer.buffer, byteBuffer.nextReadableIndex, buffer, 0, trimmedLength);
            System.arraycopy(byteBuffer.buffer, 0, buffer, trimmedLength, readableLength - trimmedLength);
         } else {
            System.arraycopy(byteBuffer.buffer, byteBuffer.nextReadableIndex, buffer, 0, readableLength);
         }
      }

      this.nextReadableIndex = 0;
      this.nextWritableIndex = (readableLength < allocationSize ? readableLength : 0);
      this.hasElements = byteBuffer.available() > 0;
   }

   public void allocate() {
      if (buffer == null) {
         buffer = byteArrayProvider.allocate(initialSize);
      }
   }

   @Override
   public void clear() {
      nextReadableIndex = 0;
      nextWritableIndex = 0;
      hasElements = false;
   }

   @Override
   public int skip(int len) {
      allocate();
      int bytesSkipped = len;

      if (len > available()) {
         bytesSkipped = available();

         nextReadableIndex = 0;
         nextWritableIndex = 0;
      } else {
         nextReadableIndex = nextReadableIndex + len < buffer.length
                 ? nextReadableIndex + len
                 : len - (buffer.length - nextReadableIndex);
      }

      if (nextReadableIndex == nextWritableIndex) {
         hasElements = false;
      }

      return bytesSkipped;
   }

   @Override
   public int available() {
      if (buffer == null) {
         return 0;
      }

      if (nextWritableIndex == nextReadableIndex && hasElements) {
         return buffer.length;
      }

      return nextWritableIndex < nextReadableIndex ? nextWritableIndex + (buffer.length - nextReadableIndex) : nextWritableIndex - nextReadableIndex;
   }

   @Override
   public int remaining() {
      if (buffer == null) {
         return initialSize;
      }

      if (nextWritableIndex == nextReadableIndex && hasElements) {
         return 0;
      }

      return nextWritableIndex < nextReadableIndex ? nextReadableIndex - nextWritableIndex : buffer.length - nextWritableIndex + nextReadableIndex;
   }

   private void grow(int minLength) {
      allocate();

      final int newSize = buffer.length + buffer.length * (minLength / buffer.length + 1);
      final byte[] newBuffer = byteArrayProvider.allocate(newSize);

      final int read = get(newBuffer, 0, newSize);
      buffer = newBuffer;

      nextWritableIndex = read;
      nextReadableIndex = 0;
      hasElements = true;
   }

   @Override
   public int put(byte[] b, int off, int len) {
      allocate();
      final int remaining = remaining();

      if (remaining < len) {
         grow(len - remaining);
      }

      if (nextWritableIndex + len > buffer.length) {
         final int trimmedLength = buffer.length - nextWritableIndex;

         System.arraycopy(b, off, buffer, nextWritableIndex, trimmedLength);
         System.arraycopy(b, off + trimmedLength, buffer, 0, len - trimmedLength);
         nextWritableIndex = len - trimmedLength;
      } else {
         System.arraycopy(b, off, buffer, nextWritableIndex, len);
         nextWritableIndex += len;
      }

      hasElements = true;

      return len;
   }

   @Override
   public int get(byte[] b, int off, int len) {
      allocate();
      final int readableLength = available() > len ? len : available();

      if (hasElements) {
         if (nextReadableIndex + readableLength > buffer.length) {
            final int trimmedLength = buffer.length - nextReadableIndex;

            System.arraycopy(buffer, nextReadableIndex, b, off, trimmedLength);
            System.arraycopy(buffer, 0, b, off + trimmedLength, readableLength - trimmedLength);
            nextReadableIndex = readableLength - trimmedLength;
         } else {
            System.arraycopy(buffer, nextReadableIndex, b, off, readableLength);
            nextReadableIndex = nextReadableIndex + readableLength <= buffer.length ? nextReadableIndex + readableLength : readableLength - (buffer.length - nextReadableIndex);
         }

         if (nextWritableIndex == nextReadableIndex) {
            hasElements = false;
         }
      }

      return readableLength;
   }

   @Override
   public void put(byte b) {
      put(new byte[]{b}, 0, 1);
   }

   @Override
   public byte get() {
      allocate();
      final byte[] singleByte = new byte[]{-1};

      if (available() > 0) {
         get(singleByte, 0, 1);
      }

      return singleByte[0];
   }

   @Override
   public int get(byte[] b) throws IOException {
      return get(b, 0, b.length);
   }

   @Override
   public int put(byte[] b) throws IOException {
      return put(b, 0, b.length);
   }

   @Override
   public ByteBuffer copy() {
      return new CyclicByteBuffer(byteArrayProvider, this);
   }

   @SuppressWarnings({"PMD.ProperCloneImplementation","PMD.CloneMethodMustImplementCloneable","CloneDoesntCallSuperClone"}) 
   @Override
   public Object clone() throws CloneNotSupportedException {
      return copy();
   }
}
