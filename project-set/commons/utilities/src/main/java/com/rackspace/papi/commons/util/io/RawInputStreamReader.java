package com.rackspace.papi.commons.util.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class RawInputStreamReader {
   
   private static final RawInputStreamReader INSTANCE = new RawInputStreamReader();
   public static final int DEFAULT_INTERNAL_BUFFER_SIZE = 4096;
   
   public static RawInputStreamReader instance() {
      return INSTANCE;
   }
   
   private RawInputStreamReader() {
   }
   
   public long copyTo(InputStream is, OutputStream os) throws IOException {
      return copyTo(is, os, DEFAULT_INTERNAL_BUFFER_SIZE);
   }
   
   public long copyTo(InputStream is, OutputStream os, int bufferSize) throws IOException {
      final byte[] internalBuffer = new byte[bufferSize];
      
      long total = 0;
      int read;
      
      while ((read = is.read(internalBuffer)) != -1) {
         os.write(internalBuffer, 0, read);
         total += read;
      }
      
      return total;
   }
   
   public byte[] readFully(InputStream is) throws IOException {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final byte[] internalBuffer = new byte[DEFAULT_INTERNAL_BUFFER_SIZE];
      
      int read;
      
      while ((read = is.read(internalBuffer)) != -1) {
         baos.write(internalBuffer, 0, read);
      }
      
      return baos.toByteArray();
   }
   
   public byte[] readFully(InputStream is, long byteLimit) throws IOException {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final byte[] internalBuffer = new byte[DEFAULT_INTERNAL_BUFFER_SIZE];
      
      int read;
      long limit = byteLimit;
      
      while ((read = is.read(internalBuffer)) != -1) {
         limit -= read;
         
         if (limit < 0) {
            throw new BufferCapacityException("Read limit reached. Max buffer size: " + limit + " bytes");
         }
         
         baos.write(internalBuffer, 0, read);
      }
      
      return baos.toByteArray();
   }
}
