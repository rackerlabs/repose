package com.rackspace.papi.commons.util.logging.apache.format.stock;

import com.rackspace.papi.commons.util.logging.apache.format.FormatterLogic;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequestHeaderHandler extends HeaderHandler implements FormatterLogic {
   private final String headerName;

   public RequestHeaderHandler(String headerName) {
      this.headerName = headerName;
   }

   @Override
   public String handle(HttpServletRequest request, HttpServletResponse response) {
      return getValues(request.getHeaders(headerName));
   }
   
}
