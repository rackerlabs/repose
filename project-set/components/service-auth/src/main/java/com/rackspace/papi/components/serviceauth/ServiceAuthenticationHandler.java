package com.rackspace.papi.components.serviceauth;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterDirector;

/**
 *
 * @author Dan Daley
 */
public class ServiceAuthenticationHandler extends AbstractFilterLogicHandler {
   private String base64EncodedCredentials;
   
   public ServiceAuthenticationHandler(String base64EncodedCredentials) {
      this.base64EncodedCredentials = base64EncodedCredentials;
   }
   
   public FilterDirector handleRequest(MutableHttpServletRequest request, MutableHttpServletResponse response) {
      request.addHeader(CommonHttpHeader.AUTHORIZATION.headerKey(), base64EncodedCredentials);

      return null;
   }

   public void handleResponse(MutableHttpServletRequest request, MutableHttpServletResponse response) {
      final String wwwAuthenticateHeader = response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.headerKey());

      switch (HttpStatusCode.fromInt(response.getStatus())) {
         case NOT_IMPLEMENTED:
            //If the service does not support delegation, return a 500
            response.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue());

            //Remove the WWW-Authenticate header if present
            if (!StringUtilities.isBlank(wwwAuthenticateHeader)) {
               response.setHeader(CommonHttpHeader.WWW_AUTHENTICATE.headerKey(), null);
            }
            break;

         case FORBIDDEN:
            //If the WWW-Authenticate header is not present or it is not set to delegated then relay a 500
            if (StringUtilities.isBlank(wwwAuthenticateHeader) && !wwwAuthenticateHeader.equalsIgnoreCase("delegated")) {
               response.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue());
            } else {
               //Remove the header
               response.setHeader(CommonHttpHeader.WWW_AUTHENTICATE.headerKey(), null);
            }

            break;
      }
   }
   
}
