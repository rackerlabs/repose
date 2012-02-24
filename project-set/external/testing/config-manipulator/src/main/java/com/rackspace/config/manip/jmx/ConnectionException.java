package com.rackspace.config.manip.jmx;

/**
 * @author fran
 */
public class ConnectionException extends RuntimeException {

   public ConnectionException(String s, Throwable throwable) {
      super(s, throwable);
   }
}
