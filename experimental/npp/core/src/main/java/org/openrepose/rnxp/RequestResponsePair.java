package org.openrepose.rnxp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author zinic
 */
public class RequestResponsePair {

   private final HttpServletRequest httpServletRequest;
   private final HttpServletResponse httpServletResponse;

   public RequestResponsePair(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
      this.httpServletRequest = httpServletRequest;
      this.httpServletResponse = httpServletResponse;
   }

   public HttpServletRequest getHttpServletRequest() {
      return httpServletRequest;
   }

   public HttpServletResponse getHttpServletResponse() {
      return httpServletResponse;
   }
}
