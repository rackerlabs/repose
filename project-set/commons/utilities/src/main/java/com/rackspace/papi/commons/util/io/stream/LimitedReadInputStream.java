package com.rackspace.papi.commons.util.io.stream;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author zinic
 */
public class LimitedReadInputStream extends InputStream {

   private final InputStream delegateStream;
   private final long readLimit;
   private long bytesRead;

   public LimitedReadInputStream(long readLimit, InputStream delegateStream) {
      this.delegateStream = delegateStream;
      this.readLimit = readLimit;
   }
   
   @Override
   public void mark(int readlimit) {
      delegateStream.mark(readlimit);
   }
   
   @Override
   public void reset() throws IOException {
      delegateStream.reset();
   }
   
   @Override
   public boolean markSupported() {
      return delegateStream.markSupported();
   }

   @Override
   public int read() throws IOException {
      if (++bytesRead > readLimit) {
         throw new ReadLimitReachedException("Read limit of " + readLimit + " for input stream has been reached");
      }

      return delegateStream.read();
   }
}
