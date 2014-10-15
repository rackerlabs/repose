package org.openrepose.commons.utils.logging.apache.format.stock;

import org.openrepose.commons.utils.logging.apache.format.FormatterLogic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class RequestHeaderHandler extends HeaderHandler implements FormatterLogic {

   public RequestHeaderHandler(String headerName, List<String> arguments) {
      super(headerName, arguments);
   }

   @Override
   public String handle(HttpServletRequest request, HttpServletResponse response) {
      return getValues(request.getHeaders(getHeaderName()));
   }
   
}
