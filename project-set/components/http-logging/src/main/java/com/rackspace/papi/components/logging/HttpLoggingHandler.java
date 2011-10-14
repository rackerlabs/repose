package com.rackspace.papi.components.logging;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import java.util.List;

/**
 *
 * @author Dan Daley
 */
public class HttpLoggingHandler extends AbstractFilterLogicHandler {
   private final List<HttpLoggerWrapper> loggers;

   public HttpLoggingHandler(List<HttpLoggerWrapper> loggers) {
      this.loggers = loggers;
   }
   
   public void handleResponse(MutableHttpServletRequest request, MutableHttpServletResponse response) {
      for (HttpLoggerWrapper loggerWrapper : loggers) {
         loggerWrapper.handle(request, response);
      }
   }
   
}
