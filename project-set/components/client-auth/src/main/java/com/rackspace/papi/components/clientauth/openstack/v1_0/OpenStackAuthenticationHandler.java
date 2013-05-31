package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.AuthGroup;
import com.rackspace.auth.AuthGroups;
import com.rackspace.auth.AuthToken;
import com.rackspace.auth.openstack.AuthenticationService;
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
public class OpenStackAuthenticationHandler extends AuthenticationHandler {

   private static final String WWW_AUTH_PREFIX = "Keystone uri=";
   private final String wwwAuthHeaderContents;
   private final AuthenticationService authenticationService;

   public OpenStackAuthenticationHandler(Configurables cfg, AuthenticationService serviceClient, AuthTokenCache cache, AuthGroupCache grpCache, AuthUserCache usrCache, EndpointsCache endpointsCache, UriMatcher uriMatcher) {
      super(cfg, cache, grpCache, usrCache, endpointsCache, uriMatcher);
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

    @Override //getting the final encoded string
    protected String getEndpointsBase64(String token, EndpointsConfiguration endpointsConfiguration) {
        return authenticationService.getBase64EndpointsStringForHeaders(token, endpointsConfiguration.getFormat());
    }

    @Override
    public void setFilterDirectorValues(String authToken, AuthToken cachableToken, Boolean delegatable,
            FilterDirector filterDirector, String extractedResult, List<AuthGroup> groups, String endpointsInBase64) {
        new OpenStackAuthenticationHeaderManager(authToken, cachableToken, delegatable, filterDirector, extractedResult,
                                                 groups, wwwAuthHeaderContents, endpointsInBase64)
                                                .setFilterDirectorValues();
    }
}
