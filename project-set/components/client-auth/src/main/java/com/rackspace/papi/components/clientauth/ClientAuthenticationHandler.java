package com.rackspace.papi.components.clientauth;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.auth.AuthModule;

import com.rackspace.papi.components.clientauth.config.ClientAuthConfig;
import com.rackspace.papi.components.clientauth.rackspace.RackspaceAuthenticationModule;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandler;
import org.slf4j.Logger;
import com.rackspace.papi.filter.logic.FilterDirector;

/**
 *
 * @author jhopper
 *
 * The purpose of this class is to handle client authentication. Multiple
 * authentication schemes may be used depending on the configuration. For
 * example, a Rackspace specific or Basic Http authentication.
 *
 */
public class ClientAuthenticationHandler extends AbstractConfiguredFilterHandler<ClientAuthConfig> {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ClientAuthenticationHandler.class);
   private AuthModule authenticationModule;

   public ClientAuthenticationHandler() {
   }

   @Override
   public void configurationUpdated(ClientAuthConfig modifiedConfig) {
      if (modifiedConfig.getRackspaceAuth() != null) {
         authenticationModule = new RackspaceAuthenticationModule(modifiedConfig.getRackspaceAuth());
      } else if (modifiedConfig.getHttpBasicAuth() == null) {
         LOG.error("Authentication module is not understood or supported. Please check your configuration.");
      }
   }

   private AuthModule getCurrentAuthModule() {
      lockConfigurationForRead();

      try {
         return authenticationModule;
      } finally {
         unlockConfigurationForRead();
      }
   }

   public FilterDirector handleRequest(MutableHttpServletRequest request, MutableHttpServletResponse response) {
      return getCurrentAuthModule().authenticate(request);
   }

   public void handleResponse(MutableHttpServletRequest request, MutableHttpServletResponse response) {
      /// The WWW Authenticate header can be used to communicate to the client
      // (since we are a proxy) how to correctly authenticate itself
      final String wwwAuthenticateHeader = response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.headerKey());

      switch (HttpStatusCode.fromInt(response.getStatus())) {
         // NOTE: We should only mutate the WWW-Authenticate header on a
         // 401 (unauthorized) or 403 (forbidden) response from the origin service
         case UNAUTHORIZED:
         case FORBIDDEN:
            updateHttpResponse(response, wwwAuthenticateHeader);
            break;
      }
   }

   private void updateHttpResponse(MutableHttpServletResponse httpResponse, String wwwAuthenticateHeader) {

      // If in the case that the origin service supports delegated authentication
      // we should then communicate to the client how to authenticate with us
      if (!StringUtilities.isBlank(wwwAuthenticateHeader) && wwwAuthenticateHeader.contains("Delegated")) {
         final String replacementWwwAuthenticateHeader = getCurrentAuthModule().getWWWAuthenticateHeaderContents();
         httpResponse.setHeader(CommonHttpHeader.WWW_AUTHENTICATE.headerKey(), replacementWwwAuthenticateHeader);
      } else {
         // In the case where authentication has failed and we did not receive
         // a delegated WWW-Authenticate header, this means that our own authentication
         // with the origin service has failed and must then be communicated as
         // a 500 (internal server error) to the client
         httpResponse.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue());
      }
   }
}
