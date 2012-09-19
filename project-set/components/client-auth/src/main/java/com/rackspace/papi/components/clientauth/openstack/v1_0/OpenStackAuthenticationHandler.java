package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.AuthGroup;
import com.rackspace.auth.AuthGroups;
import com.rackspace.auth.AuthToken;
import com.rackspace.auth.FullAuthInfo;
import com.rackspace.auth.openstack.AuthenticationService;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientauth.common.*;
import com.rackspace.papi.filter.logic.FilterDirector;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * @author fran
 */
public class OpenStackAuthenticationHandler extends AuthenticationHandler {

   private static final Logger LOG = LoggerFactory.getLogger(OpenStackAuthenticationHandler.class);
   private static final String WWW_AUTH_PREFIX = "Keystone uri=";
   private final String wwwAuthHeaderContents;
   private final AuthenticationService authenticationService;
   private final EndpointListCacheImpl endpointListCache;

   public OpenStackAuthenticationHandler(Configurables cfg, AuthenticationService serviceClient, AuthTokenCache cache, AuthGroupCache grpCache, UriMatcher uriMatcher,
           EndpointListCacheImpl endpointListCache) {
      super(cfg, cache, grpCache, uriMatcher);
      this.authenticationService = serviceClient;
      this.wwwAuthHeaderContents = WWW_AUTH_PREFIX + cfg.getAuthServiceUri();
      this.endpointListCache = endpointListCache;
   }

   @Override
   public FullAuthInfo validateToken(ExtractorResult<String> account, String token) {

      FullAuthInfo accountInfo;

      if (account != null) {
         accountInfo = authenticationService.validateToken(account.getResult(), token);
      } else {
         accountInfo = authenticationService.validateToken(null, token);
      }
      if (accountInfo.getEndpoints() != null) {
         try {
            endpointListCache.cacheEndpointsForToken(token, accountInfo.getEndpoints());
         } catch (IOException io) {
            LOG.error("Caching Failure: " + io.getMessage(), io);
         }
      }

      return accountInfo;
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
      new OpenStackAuthenticationHeaderManager(authToken, cachableToken, delegatable, filterDirector, extractedResult, groups).setFilterDirectorValues();
   }
}
