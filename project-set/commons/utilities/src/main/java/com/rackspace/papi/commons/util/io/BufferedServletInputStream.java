package com.rackspace.papi.commons.util.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletInputStream;

public class BufferedServletInputStream extends ServletInputStream {
   private final BufferedInputStream inputStream;
   
   public BufferedServletInputStream(InputStream inputStream) {
      this.inputStream = new BufferedInputStream(inputStream);
   }
   
   @Override
   public int available() throws IOException {
      return inputStream.available();
   }
   
   @Override
   public void close() throws IOException {
      inputStream.close();
   }
   
   @Override
   public void reset() throws IOException {
      inputStream.reset();
   }

   @Override
   public int read() throws IOException {
      return inputStream.read();
   }
   
   @Override
   public void mark(int readlimit) {
      inputStream.mark(readlimit);
   }
   
   @Override
   public boolean markSupported() {
      return inputStream.markSupported();
   }
   
}
