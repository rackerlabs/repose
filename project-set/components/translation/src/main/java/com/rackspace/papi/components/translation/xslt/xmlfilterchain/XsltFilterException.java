package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

public class XsltFilterException extends RuntimeException {
   public XsltFilterException(String message) {
      super(message);
   }
   
   public XsltFilterException(Throwable cause) {
      super(cause);
   }
   
   public XsltFilterException(String message, Throwable cause) {
      super(message, cause);
   }
   
}
