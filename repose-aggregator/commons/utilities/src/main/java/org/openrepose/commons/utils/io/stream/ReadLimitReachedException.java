package org.openrepose.commons.utils.io.stream;

import java.io.IOException;

/**
 *
 * @author zinic
 */
public class ReadLimitReachedException extends IOException {

   public ReadLimitReachedException(String string) {
      super(string);
   }
}
