package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.logging.ExceptionLogger;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: joshualockwood Date: May 19, 2011 Time: 10:02:36 AM
 */
public class HttpServletHelper {

   private HttpServletHelper() {
   }

   public static void verifyRequestAndResponse(Logger logger, ServletRequest request, ServletResponse response)
           throws ServletException {
      if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
         throw new ExceptionLogger(logger).newException(
                 "Please verify that the container you are using supports Servlet API 3.0 and supports HttpServlet requests and responses",
                 ServletException.class);
      }

   }
}
