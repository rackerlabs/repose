package com.rackspace.papi.commons.util.io;

import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;

import java.io.IOException;
import javax.servlet.ServletInputStream;

public class ByteBufferInputStream extends ServletInputStream {

   private final ByteBuffer sharedBuffer;
   private volatile boolean closed;

   public ByteBufferInputStream(ByteBuffer sharedBuffer) {
      this.sharedBuffer = sharedBuffer;

      closed = false;
   }

   private void checkForClosedStream() throws IOException {
      if (closed) {
         throw new IOException("InputStream has been closed. Futher operations are prohibited");
      }
   }

   @Override
   public boolean markSupported() {
      return false;
   }

   @Override
   public int available() throws IOException {
      checkForClosedStream();

      return sharedBuffer.available();
   }

   @Override
   public void close() throws IOException {
      checkForClosedStream();

      closed = true;
   }

   @Override
   public int read() throws IOException {
      checkForClosedStream();

      return sharedBuffer.get();
   }

   @Override
   public int read(byte[] b) throws IOException {
      checkForClosedStream();

      return normalizeBufferReadLength(sharedBuffer.get(b));
   }

   @Override
   public int read(byte[] b, int off, int len) throws IOException {
      checkForClosedStream();

      return normalizeBufferReadLength(sharedBuffer.get(b, off, len));
   }

   private int normalizeBufferReadLength(int readLength) {
      return readLength == 0 ? -1 : readLength;
   }

   @Override
   public long skip(long n) throws IOException {
      checkForClosedStream();

      long skipped = 0, skippedTotal = 0, c = n;

      if (c > 0) {
         do {
            int toSkip = c > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) c;
            skipped = sharedBuffer.skip(toSkip);

            skippedTotal += skipped;
            c -= skipped;
         } while (c > 0 && skipped > 0);
      }

      return skippedTotal;
   }
}
