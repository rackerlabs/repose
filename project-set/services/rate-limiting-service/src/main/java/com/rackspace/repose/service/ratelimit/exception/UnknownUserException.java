package com.rackspace.repose.service.ratelimit.exception;

public class UnknownUserException extends RuntimeException {

   public UnknownUserException(String message) {
      super(message);
   }
}
