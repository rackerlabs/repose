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

      @Override
      public int read() throws IOException {
         int result = -1;

         while (result < 0 && index < streams.length) {
            result = streams[index].read();

            if (result < 0) {
               streams[index++].close();
            }
         }

         return result;
      }
   
}
