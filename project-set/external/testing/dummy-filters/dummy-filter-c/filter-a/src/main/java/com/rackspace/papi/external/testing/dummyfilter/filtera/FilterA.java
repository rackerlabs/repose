
package com.rackspace.papi.external.testing.dummyfilter.filtera;

import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterA implements Filter {

   private static final Logger LOG = LoggerFactory.getLogger(FilterA.class);

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      //new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
      HttpServletHelper.verifyRequestAndResponse(LOG, request, response);

      final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) request);
      final MutableHttpServletResponse mutableHttpResponse = MutableHttpServletResponse.wrap(mutableHttpRequest, (HttpServletResponse) response);
      
      mutableHttpRequest.addHeader("Filter AAA", "AAA");
      
      chain.doFilter(mutableHttpRequest, mutableHttpResponse);
   }

   @Override
   public void destroy() {
   }

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {
      LOG.info("Initializing Dummy Filter A ");

   }
}
