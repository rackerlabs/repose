package com.rackspace.papi.components.ratelimit.exception;

public class RateLimitingSerializationException extends RuntimeException {

   public RateLimitingSerializationException(String message, Throwable t) {
      super(message, t);
   }
}
