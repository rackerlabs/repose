package org.openrepose.core.filter.logic.impl;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.FilterLogicHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 * Responsible for calling the specific file logic based on filter actions 
 */
public class FilterLogicHandlerDelegate {

   private static final Logger LOG = LoggerFactory.getLogger(FilterLogicHandlerDelegate.class);
   private final ServletRequest request;
   private final ServletResponse response;
   private final FilterChain chain;

   public FilterLogicHandlerDelegate(ServletRequest request, ServletResponse response, FilterChain chain) {
      this.request = request;
      this.response = response;
      this.chain = chain;
   }

   public void doFilter(FilterLogicHandler handler) throws IOException, ServletException {
      final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) request);
      final MutableHttpServletResponse mutableHttpResponse = MutableHttpServletResponse.wrap(mutableHttpRequest, (HttpServletResponse) response);

      if (handler == null) {
         mutableHttpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Error creating filter chain, check your configuration files.");
         LOG.error("Failed to startup Repose with your configuration. Please check your configuration files and your artifacts directory. Unable to create filter chain.");
         
      } else {
         final FilterDirector requestFilterDirector = handler.handleRequest(mutableHttpRequest, mutableHttpResponse);

         switch (requestFilterDirector.getFilterAction()) {
            case NOT_SET:
               chain.doFilter(request, response);
               break;

            case PASS:
               requestFilterDirector.applyTo(mutableHttpRequest);
               chain.doFilter(mutableHttpRequest, mutableHttpResponse);
               break;

            case PROCESS_RESPONSE:
               requestFilterDirector.applyTo(mutableHttpRequest);
               chain.doFilter(mutableHttpRequest, mutableHttpResponse);

               final FilterDirector responseDirector = handler.handleResponse(mutableHttpRequest, mutableHttpResponse);
               responseDirector.applyTo(mutableHttpResponse);
               break;

            case RETURN:
               requestFilterDirector.applyTo(mutableHttpResponse);
               break;
         }
      }
   }
}
