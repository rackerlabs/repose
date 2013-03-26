package com.rackspace.papi.commons.util.io;

import com.rackspace.papi.commons.util.io.charset.CharacterSets;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class InputStreamMerger extends InputStream {

   public static InputStream merge(InputStream... streams) {
      return new InputStreamMerger(streams);
   }

   public static InputStream wrap(String string) {
      return new ByteArrayInputStream(string.getBytes(CharacterSets.UTF_8));
   }
   private int index = 0;
   private final InputStream[] streams;

   private InputStreamMerger(InputStream... streams) {
      this.streams = streams;
   }

   private boolean haveCurrentStream() {
      while (index < streams.length && streams[index] == null) {
         // skip null streams
         index++;
      }

      return index < streams.length;
   }

   private InputStream getCurrentStream() {
      return streams[index];
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
      for (InputStream stream : streams) {
         if (stream != null) {
            stream.close();
         }
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
      long skipped = 0;
      long totalSkipped = 0;
      long remaining = count;
      while (haveCurrentStream() && remaining > 0) {
         skipped = getCurrentStream().skip(remaining);
         remaining -= skipped;
         totalSkipped += skipped;

         if (skipped < count && getCurrentStream().available() <= 0) {
            // Skip to next stream to continue skipping
            index++;
         }
      }

      return totalSkipped;

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
      return read(b, 0, b.length);
   }

   @Override
   public int read(byte[] b, int off, int len) throws IOException {
      int totalRead = 0;
      int remaining = len;

      while (remaining > 0 && haveCurrentStream()) {
         int result = getCurrentStream().read(b, totalRead + off, remaining);

         if (result > 0) {
            // read data successfully.  Update pointers.
            totalRead += result;
            remaining -= result;
         } else {
            // try reading from the next stream
            index++;
         }
      }

      return totalRead > 0 ? totalRead : -1;
   }
}
