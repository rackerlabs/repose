package com.rackspace.papi.commons.util.io.stream;

import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author zinic
 */
public class ServletInputStreamWrapper extends ServletInputStream {

   private final InputStream is;

   public ServletInputStreamWrapper(InputStream is) {
      this.is = is;
   }

   @Override
   public int read() throws IOException {
      return is.read();
   }
}
