package com.rackspace.papi.components.translation.xslt.handlerchain;

public class XsltHandlerException extends RuntimeException {
   public XsltHandlerException(String message) {
      super(message);
   }
   
   public XsltHandlerException(Throwable cause) {
      super(cause);
   }
   
   public XsltHandlerException(String message, Throwable cause) {
      super(message, cause);
   }
   
}
