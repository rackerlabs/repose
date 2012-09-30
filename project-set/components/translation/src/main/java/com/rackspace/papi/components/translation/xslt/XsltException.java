package com.rackspace.papi.components.translation.xslt;

public class XsltException extends RuntimeException {
   public XsltException(String message) {
      super(message);
   }
   
   public XsltException(Throwable cause) {
      super(cause);
   }
   
   public XsltException(String message, Throwable cause) {
      super(message, cause);
   }
   
}
