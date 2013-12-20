package com.rackspace.papi.components.translation.postprocessor;

public class PostProcessorException extends RuntimeException {
   public PostProcessorException(String message) {
      super(message);
   }
   
   public PostProcessorException(Throwable cause) {
      super(cause);
   }
   
   public PostProcessorException(String message, Throwable cause) {
      super(message, cause);
   }
   
}
