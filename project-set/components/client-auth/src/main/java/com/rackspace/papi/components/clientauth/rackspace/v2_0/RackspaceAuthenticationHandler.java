package com.rackspace.papi.components.clientauth.rackspace.v2_0;

import com.rackspace.auth.v2_0.Account;
import com.rackspace.auth.v2_0.AuthenticationServiceClient;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.papi.components.clientauth.rackspace.config.RackspaceAuthV20;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;

import com.rackspace.papi.auth.AuthModule;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import org.openstack.docs.identity.api.v2.Token;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * @author fran
 */
public class RackspaceAuthenticationHandler extends AbstractFilterLogicHandler implements AuthModule {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RackspaceAuthenticationHandler.class);
   private final AuthenticationServiceClient authenticationService;
   private final RackspaceAuthV20 cfg;
   private final AccountUsernameExtractor accountUsernameExtractor;

   public RackspaceAuthenticationHandler(RackspaceAuthV20 cfg) {
      this.authenticationService = new AuthenticationServiceClient(cfg.getAuthenticationServer().getUri(), cfg.getAuthenticationServer().getUsername(), cfg.getAuthenticationServer().getPassword());
      this.cfg = cfg;
      this.accountUsernameExtractor = new AccountUsernameExtractor(cfg.getAccountMapping());
   }

    @Override
    public String getWWWAuthenticateHeaderContents() {
        return "TODO: What should this be for Cloud Auth 2.0?";
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        return this.authenticate(request);
    }

    @Override
    public FilterDirector authenticate(HttpServletRequest request) {
        final FilterDirector filterDirector = new FilterDirectorImpl();
      filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED);
      filterDirector.setFilterAction(FilterAction.USE_MESSAGE_SERVICE);

      final String authToken = request.getHeader(CommonHttpHeader.AUTH_TOKEN.headerKey());
      final Account account = accountUsernameExtractor.extract(request.getRequestURL().toString());

      try {
        Token token = authenticationService.validateToken(account, authToken);
        AuthenticationHeaderManager headerManager = new AuthenticationHeaderManager(token, cfg.isDelegatable(), filterDirector, account.getUsername());
        headerManager.setFilterDirectorValues();  
          
      } catch (Exception ex) {
        LOG.error("Failure in auth: " + ex.getMessage(), ex);
        filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);    
      }

      return filterDirector;
    }

    private Groups getGroups(String userId, FilterDirector filterDirector) {
        Groups groups = null;

        try {
            groups = authenticationService.getGroups(userId);
        } catch (Exception ex) {
            LOG.error("Failure in auth: " + ex.getMessage(), ex);
            filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
        }

        return groups;
    }

    @Override
    public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {
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

      // TODO: Do we need to return a valid FilterDirector here?
      return null;
    }

    private void updateHttpResponse(ReadableHttpServletResponse httpResponse, String wwwAuthenticateHeader) {

      // If in the case that the origin service supports delegated authentication
      // we should then communicate to the client how to authenticate with us
      if (!StringUtilities.isBlank(wwwAuthenticateHeader) && wwwAuthenticateHeader.contains("Delegated")) {
         final String replacementWwwAuthenticateHeader = getWWWAuthenticateHeaderContents();
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
