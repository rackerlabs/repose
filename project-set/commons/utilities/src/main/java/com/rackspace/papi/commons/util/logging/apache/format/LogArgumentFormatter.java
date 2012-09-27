package com.rackspace.papi.commons.util.logging.apache.format;

import com.rackspace.papi.commons.util.logging.apache.constraint.StatusCodeConstraint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LogArgumentFormatter implements FormatArgumentHandler {

   private StatusCodeConstraint statusCodeConstraint;
   private FormatterLogic logic;

   public void setStatusCodeConstraint(StatusCodeConstraint statusCodeConstraint) {
      this.statusCodeConstraint = statusCodeConstraint;
   }

   public void setLogic(FormatterLogic logic) {
      this.logic = logic;
   }

   @Override
   public String format(HttpServletRequest request, HttpServletResponse response) {
      boolean pass = true;

      if (statusCodeConstraint != null) {
         pass = statusCodeConstraint.pass(response);
      }

      return pass && getLogic() != null ? getLogic().handle(request, response) : "-";
   }

   public FormatterLogic getLogic() {
      return logic;
   }
}
