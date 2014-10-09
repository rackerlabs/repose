package org.openrepose.commons.utils.logging.apache.format.stock;

import org.openrepose.commons.utils.logging.apache.format.FormatterLogic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CanonicalPortHandler implements FormatterLogic {

   @Override
   public String handle(HttpServletRequest request, HttpServletResponse response) {
      return String.valueOf(request.getLocalPort());
   }
}
