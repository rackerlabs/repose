package com.rackspace.papi.commons.util.logging.apache.format.stock;

import com.rackspace.papi.commons.util.logging.apache.format.FormatterLogic;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequestHeaderHandler extends HeaderHandler implements FormatterLogic {
   private final String headerName;
   private final List<String> arguments;

   public RequestHeaderHandler(String headerName, List<String> arguments) {
      this.headerName = headerName;
      this.arguments = arguments;
   }

   @Override
   public String handle(HttpServletRequest request, HttpServletResponse response) {
      return getValues(request.getHeaders(headerName));
   }
   
}
