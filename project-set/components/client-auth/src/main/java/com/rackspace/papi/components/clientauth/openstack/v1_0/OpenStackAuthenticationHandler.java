package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.AuthGroup;
import com.rackspace.auth.AuthGroups;
import com.rackspace.auth.AuthToken;
import com.rackspace.auth.openstack.AuthenticationService;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientauth.common.*;
import com.rackspace.papi.filter.logic.FilterDirector;

import java.util.List;

/**
 * @author fran
 */
public class OpenStackAuthenticationHandler extends AuthenticationHandler {

   private static final String WWW_AUTH_PREFIX = "Keystone uri=";
   private final String wwwAuthHeaderContents;
   private final AuthenticationService authenticationService;

   public OpenStackAuthenticationHandler(Configurables cfg, AuthenticationService serviceClient, AuthTokenCache cache, AuthGroupCache grpCache, UriMatcher uriMatcher) {
      super(cfg, cache, grpCache, uriMatcher);
      this.authenticationService = serviceClient;
      this.wwwAuthHeaderContents = WWW_AUTH_PREFIX + cfg.getAuthServiceUri();
   }

   @Override
   public AuthToken validateToken(ExtractorResult<String> account, String token) {
      return account != null ? authenticationService.validateToken(account.getResult(), token) : authenticationService.validateToken(null, token);
   }

   @Override
   public AuthGroups getGroups(String group) {
      return authenticationService.getGroups(group);
   }

   @Override
   public FilterDirector processResponse(ReadableHttpServletResponse response) {
      return new OpenStackResponseHandler(response, wwwAuthHeaderContents).handle();
   }

   @Override
   public void setFilterDirectorValues(String authToken, AuthToken cachableToken, Boolean delegatable, FilterDirector filterDirector, String extractedResult, List<AuthGroup> groups) {
      new OpenStackAuthenticationHeaderManager(authToken, cachableToken, delegatable, filterDirector, extractedResult, groups, wwwAuthHeaderContents).setFilterDirectorValues();
   }
}
