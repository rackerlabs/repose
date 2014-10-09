package org.openrepose.filters.ratelimiting.exception;

public class RateLimitingSerializationException extends RuntimeException {

   public RateLimitingSerializationException(String message, Throwable t) {
      super(message, t);
   }
}
