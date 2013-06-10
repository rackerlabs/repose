package com.rackspace.papi.components.clientauth.rackspace.v1_1;

import com.rackspace.auth.AuthGroup;
import com.rackspace.auth.AuthGroups;
import com.rackspace.auth.AuthToken;
import com.rackspace.auth.rackspace.AuthenticationService;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientauth.common.AuthGroupCache;
import com.rackspace.papi.components.clientauth.common.AuthTokenCache;
import com.rackspace.papi.components.clientauth.common.AuthUserCache;
import com.rackspace.papi.components.clientauth.common.AuthenticationHandler;
import com.rackspace.papi.components.clientauth.common.Configurables;
import com.rackspace.papi.components.clientauth.common.EndpointsCache;
import com.rackspace.papi.components.clientauth.common.EndpointsConfiguration;
import com.rackspace.papi.components.clientauth.common.UriMatcher;
import com.rackspace.papi.filter.logic.FilterDirector;

import java.util.List;

/**
 * @author fran
 */
public class RackspaceAuthenticationHandler extends AuthenticationHandler {

   private final AuthenticationService authenticationService;
   private static final String WWW_AUTH_HEADER_CONTENTS = "RackAuth Realm=\"API Realm\"";

   public RackspaceAuthenticationHandler(Configurables cfg, AuthenticationService authenticationService, AuthTokenCache cache, AuthGroupCache grpCache, AuthUserCache usrCache,
           EndpointsCache endpointsCache, UriMatcher uriMatcher) {
      super(cfg, cache, grpCache, usrCache, endpointsCache, uriMatcher);
      this.authenticationService = authenticationService;
   }

   @Override
   public AuthToken validateToken(ExtractorResult<String> account, String token) {
      return authenticationService.validateToken(account, token);
   }

   @Override
   public AuthGroups getGroups(String group) {
      return authenticationService.getGroups(group);
   }

   @Override
   public FilterDirector processResponse(ReadableHttpServletResponse response) {
      return new RackspaceResponseHandler(response, WWW_AUTH_HEADER_CONTENTS).handle();
   }

    @Override
    protected String getEndpointsBase64(String token, EndpointsConfiguration endpointsConfiguration) {
        return null;  //This is not implemented in Rackspace auth, only openstack.
    }

   @Override
   public void setFilterDirectorValues(String authToken, AuthToken cachableToken, Boolean delegatable, FilterDirector filterDirector, String extractedResult, List<AuthGroup> groups, String endpointsBase64) {
      new RackspaceAuthenticationHeaderManager(cachableToken != null, delegatable, filterDirector, extractedResult, groups).setFilterDirectorValues();
   }
}
