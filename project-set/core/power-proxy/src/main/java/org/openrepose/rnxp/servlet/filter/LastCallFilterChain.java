package org.openrepose.rnxp.servlet.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author zinic
 */
public class LastCallFilterChain implements FilterChain {

   private HttpServletRequest lastRequestObjectPassed;
   private HttpServletResponse lastResponseObjectPassed;

   @Override
   public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
      lastRequestObjectPassed = (HttpServletRequest) request;
      lastResponseObjectPassed = (HttpServletResponse) response;
   }

   public HttpServletRequest getLastRequestObjectPassed() {
      return lastRequestObjectPassed;
   }

   public HttpServletResponse getLastResponseObjectPassed() {
      return lastResponseObjectPassed;
   }
}
