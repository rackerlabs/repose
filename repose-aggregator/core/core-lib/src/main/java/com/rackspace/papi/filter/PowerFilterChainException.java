package com.rackspace.papi.filter;

/**
 *
 * @author Dan Daley
 */
public class PowerFilterChainException extends Exception {
   public PowerFilterChainException(String message) {
      super(message);
   }
   
   public PowerFilterChainException(String message, Throwable cause) {
      super(message, cause);
   }
   
}
