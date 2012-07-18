package com.rackspace.repose.service.ratelimit.exception;

public class RateLimitingConfigurationException extends RuntimeException {

   public RateLimitingConfigurationException(String message) {
      super(message);
   }
}
