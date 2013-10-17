package com.rackspace.papi.commons.util.logging.apache.format.stock;

import com.rackspace.papi.commons.util.logging.apache.format.FormatterLogic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class ResponseHeaderHandler extends HeaderHandler implements FormatterLogic {

   public ResponseHeaderHandler(String headerName, List<String> arguments) {
      super(headerName, arguments);
   }
   
   @Override
   public String handle(HttpServletRequest request, HttpServletResponse response) {
      return getValues(response.getHeaders(getHeaderName()));
   }
}
