package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.auth.openstack.ids.CachableUserInfo;
import com.rackspace.auth.openstack.ids.OpenStackAuthenticationService;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;

import com.rackspace.papi.auth.AuthModule;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientauth.UserAuthTokenCache;
import com.rackspace.papi.components.clientauth.openstack.config.OpenstackAuth;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.io.IOException;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * @author fran
 */
public class OpenStackAuthenticationHandler extends AbstractFilterLogicHandler implements AuthModule {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(OpenStackAuthenticationHandler.class);
   private final OpenStackAuthenticationService authenticationService;
   private boolean delegatable;
   private final String authServiceUri;
   private final KeyedRegexExtractor<Object> keyedRegexExtractor;
   private final UserAuthTokenCache<CachableUserInfo> cache;

   public OpenStackAuthenticationHandler(OpenstackAuth cfg, OpenStackAuthenticationService serviceClient, KeyedRegexExtractor keyedRegexExtractor, UserAuthTokenCache cache) {
      this.authenticationService = serviceClient;
      this.delegatable = cfg.isDelegatable();
      this.authServiceUri = cfg.getIdentityService().getUri();
      this.keyedRegexExtractor = keyedRegexExtractor;
      this.cache = cache;
   }

   @Override
   public String getWWWAuthenticateHeaderContents() {
      return "Keystone uri=" + authServiceUri;
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      return this.authenticate(request);
   }

   @Override
   public FilterDirector authenticate(HttpServletRequest request) {
      final FilterDirector filterDirector = new FilterDirectorImpl();
      filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED);
      filterDirector.setFilterAction(FilterAction.RETURN);

      final String authToken = request.getHeader(CommonHttpHeader.AUTH_TOKEN.toString());
      final ExtractorResult<Object> account = keyedRegexExtractor.extract(request.getRequestURI());
      CachableUserInfo user = null;

      if ((!StringUtilities.isBlank(authToken) && account != null)) {
         user = checkUserCache(account.getResult(), authToken);

         if (user == null) {
            try {
               user = authenticationService.validateToken(account.getResult(), authToken);
               cacheUserInfo(user);
            } catch (Exception ex) {
               LOG.error("Failure in auth: " + ex.getMessage(), ex);
               filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
            }
         }
      }

      Groups groups = null;
      if (user != null) {
         groups = authenticationService.getGroups(user.getUsername());
      }

      final AuthenticationHeaderManager headerManager = new AuthenticationHeaderManager(authToken, user, delegatable, filterDirector, account == null ? "" : account.getResult(), groups, request);
      headerManager.setFilterDirectorValues();

      return filterDirector;
   }
   
   private CachableUserInfo checkUserCache(String userId, String token) {
      if (cache == null) {
         return null;
      }
      
      return cache.getUserToken(userId, token);
   }

   private void cacheUserInfo(CachableUserInfo user) {
      if (user == null || cache ==  null) {
         return;
      }

      try {
         cache.storeToken(user.getUserId(), user, user.tokenTtl().intValue());
      } catch (IOException ex) {
         LOG.warn("Unable to cache user token information: " + user.getUserId());
      }
   }

   @Override
   public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {
      FilterDirector myDirector = new FilterDirectorImpl();

      /// The WWW Authenticate header can be used to communicate to the client
      // (since we are a proxy) how to correctly authenticate itself
      final String wwwAuthenticateHeader = response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString());

      switch (HttpStatusCode.fromInt(response.getStatus())) {
         // NOTE: We should only mutate the WWW-Authenticate header on a
         // 401 (unauthorized) or 403 (forbidden) response from the origin service
         case UNAUTHORIZED:
         case FORBIDDEN:
            myDirector = updateHttpResponse(myDirector, wwwAuthenticateHeader);
            break;
         case NOT_IMPLEMENTED:
            if ((!StringUtilities.isBlank(wwwAuthenticateHeader) && wwwAuthenticateHeader.contains("Delegated"))) {
               myDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
               LOG.error("Repose authentication component is configured as delegetable but origin service does not support delegated mode.");
            } else {
               myDirector.setResponseStatus(HttpStatusCode.NOT_IMPLEMENTED);
            }
            break;
      }

      return myDirector;
   }

   private FilterDirector updateHttpResponse(FilterDirector director, String wwwAuthenticateHeader) {
      // If in the case that the origin service supports delegated authentication
      // we should then communicate to the client how to authenticate with us
      if (!StringUtilities.isBlank(wwwAuthenticateHeader) && wwwAuthenticateHeader.contains("Delegated")) {
         final String replacementWwwAuthenticateHeader = getWWWAuthenticateHeaderContents();
         director.responseHeaderManager().putHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString(), replacementWwwAuthenticateHeader);
      } else {
         // In the case where authentication has failed and we did not receive
         // a delegated WWW-Authenticate header, this means that our own authentication
         // with the origin service has failed and must then be communicated as
         // a 500 (internal server error) to the client
         director.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
      }

      return director;
   }
}
