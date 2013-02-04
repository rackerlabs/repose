package com.rackspace.papi.components.translation.httpx;

public class HttpxException extends RuntimeException {
  public HttpxException() {
    super();
  }
  
  public HttpxException(String message) {
    super(message);
  }
  
  public HttpxException(String message, Throwable cause) {
    super(message, cause);
  }
  
  public HttpxException(Throwable cause) {
    super(cause);
  }
}
