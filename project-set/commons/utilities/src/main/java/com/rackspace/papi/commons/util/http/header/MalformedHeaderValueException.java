package com.rackspace.papi.commons.util.http.header;

/**
 *
 * @author zinic
 */
public class MalformedHeaderValueException extends RuntimeException {

   public MalformedHeaderValueException(String string, Throwable thrwbl) {
      super(string, thrwbl);
   }

   public MalformedHeaderValueException(String string) {
      super(string);
   }
}
