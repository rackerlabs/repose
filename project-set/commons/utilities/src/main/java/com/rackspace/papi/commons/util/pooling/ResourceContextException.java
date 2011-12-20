package com.rackspace.papi.commons.util.pooling;

public class ResourceContextException extends RuntimeException {

   public ResourceContextException(String string) {
      super(string);
   }

   public ResourceContextException(String message, Throwable cause) {
      super(message, cause);
   }
}
