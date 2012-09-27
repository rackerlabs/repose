package com.rackspace.papi.commons.util.classloader.ear;

public class EarProcessingException extends RuntimeException {

   public static final String ERROR_MESSAGE = "Unexpected error caught while attempting to read archive descriptor";

   public EarProcessingException(Throwable cause) {
      super(ERROR_MESSAGE, cause);
   }
}
