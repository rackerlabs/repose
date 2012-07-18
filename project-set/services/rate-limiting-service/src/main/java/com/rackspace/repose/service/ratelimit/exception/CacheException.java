package com.rackspace.repose.service.ratelimit.exception;

public class CacheException extends RuntimeException {

   public CacheException(String message, Throwable t) {
      super(message, t);
   }
}
