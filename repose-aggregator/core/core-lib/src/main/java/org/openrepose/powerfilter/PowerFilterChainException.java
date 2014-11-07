package org.openrepose.powerfilter;

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
