package com.rackspace.papi.commons.util.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamMerger extends InputStream {
      public static InputStream merge(InputStream... streams) {
         return new InputStreamMerger(streams);
      }
      
      public static InputStream wrap(String string) {
         return new ByteArrayInputStream(string.getBytes());
      }

      private int index = 0;
      private final InputStream[] streams;

      private InputStreamMerger(InputStream... streams) {
         this.streams = streams;
      }
      
      private boolean haveCurrentStream() {
         return index < streams.length;
      }
      
      private InputStream getCurrentStream() {
         if (haveCurrentStream()) {
            return streams[index];
         }
         
         return null;
      }

      @Override
      public int available() throws IOException {
         if (haveCurrentStream()) {
            return getCurrentStream().available();
         }
         
         return 0;
      }
      
      @Override
      public void close() throws IOException {
         for (InputStream stream: streams) {
            stream.close();
         }
      }
      
      @Override
      public void mark(int readlimit) {
         // not supported
      }
      
      @Override
      public boolean markSupported() {
         return false;
      }

      @Override
      public void reset() throws IOException {
         throw new IOException("Reset not supported");
      }
      
      @Override
      public long skip(long count) throws IOException {
         if (haveCurrentStream()) {
            return getCurrentStream().skip(count);
         }
         
         return 0;
         
      }
      
      @Override
      public int read() throws IOException {
         int result = -1;

         while (result < 0 && haveCurrentStream()) {
            result = getCurrentStream().read();

            if (result < 0) {
               index++;
            }
         }

         return result;
      }

      
      @Override
      public int read(byte[] b) throws IOException {
         int result = -1;
         
         while (result == -1 && haveCurrentStream()) {
            result = getCurrentStream().read(b);
            if (result < 0) {
               index++;
            }
         }
         
         return result;
      }
      
      @Override
      public int read(byte[] b, int off, int len) throws IOException {
         int result = -1;
         
         while (result == -1 && haveCurrentStream()) {
            result = getCurrentStream().read(b, off, len);
            if (result < 0) {
               index++;
            }
         }
         
         return result;
      }

}
