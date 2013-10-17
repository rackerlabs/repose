package com.rackspace.papi.commons.util.io;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA. User: joshualockwood Date: 6/27/11 Time: 9:24 AM
 */
public abstract class FileReader {

   public String read() throws IOException {
      checkPreconditions();

      final StringBuilder lineBuffer = new StringBuilder();
      final BufferedReader reader = getReader();

      String line;

      while ((line = reader.readLine()) != null) {
         lineBuffer.append(line);
      }

      reader.close();

      return lineBuffer.toString();
   }

   protected abstract void checkPreconditions() throws IOException;

   protected abstract BufferedReader getReader() throws IOException;
}
