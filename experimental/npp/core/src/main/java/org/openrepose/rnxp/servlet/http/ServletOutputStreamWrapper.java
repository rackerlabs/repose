package org.openrepose.rnxp.servlet.http;

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author zinic
 */
public class ServletOutputStreamWrapper<T extends OutputStream> extends javax.servlet.ServletOutputStream {

   private final T outputStream;

   public ServletOutputStreamWrapper(T outputStream) {
      this.outputStream = outputStream;
   }

   public T getWrappedOutputStream() {
      return outputStream;
   }
   
   @Override
   public void close() throws IOException {
      outputStream.close();
   }

   @Override
   public void flush() throws IOException {
      outputStream.flush();
   }

   @Override
   public void write(int i) throws IOException {
      outputStream.write(i);
   }

   @Override
   public void write(byte[] bytes) throws IOException {
      outputStream.write(bytes);
   }

   @Override
   public void write(byte[] bytes, int i, int i1) throws IOException {
      outputStream.write(bytes, i, i1);
   }
}
