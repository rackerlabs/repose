package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.auth.openstack.ids.CachableUserInfo;
import com.rackspace.auth.openstack.ids.OpenStackAuthenticationService;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.papi.components.clientauth.AuthModule;
import com.rackspace.papi.components.clientauth.common.UriMatcher;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientauth.common.UserAuthTokenCache;
import com.rackspace.papi.components.clientauth.openstack.config.OpenstackAuth;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.io.IOException;

import com.sun.jersey.api.client.ClientHandlerException;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * @author fran
 */
public class OpenStackAuthenticationHandler extends AbstractFilterLogicHandler implements AuthModule {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(OpenStackAuthenticationHandler.class);
   private final OpenStackAuthenticationService authenticationService;
   private boolean delegable;
   private final String authServiceUri;
   private final KeyedRegexExtractor<Object> keyedRegexExtractor;
   private final UserAuthTokenCache<CachableUserInfo> cache;
   private final UriMatcher uriMatcher;
   private boolean  includeQueryParams;

   public OpenStackAuthenticationHandler(OpenstackAuth cfg, OpenStackAuthenticationService serviceClient, KeyedRegexExtractor keyedRegexExtractor, UserAuthTokenCache cache, UriMatcher uriMatcher) {
      this.authenticationService = serviceClient;
      this.delegable = cfg.isDelegable();
      this.authServiceUri = cfg.getIdentityService().getUri();
      this.keyedRegexExtractor = keyedRegexExtractor;
      this.cache = cache;
      this.uriMatcher = uriMatcher;
      this.includeQueryParams = cfg.isIncludeQueryParams();
   }

   @Override
   public String getWWWAuthenticateHeaderContents() {
      return "Keystone uri=" + authServiceUri;
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      FilterDirector filterDirector = new FilterDirectorImpl();
      filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED);
      filterDirector.setFilterAction(FilterAction.RETURN);

      String uri = request.getRequestURI();
      LOG.debug("Uri is " + uri);
      if (uriMatcher.isUriOnWhiteList(request.getRequestURI())) {
         filterDirector.setFilterAction(FilterAction.PASS);
         LOG.debug("Uri is on whitelist!  Letting request pass through.");
      } else {
         filterDirector = this.authenticate(request);
      }

      return filterDirector;
   }

   @Override
   public FilterDirector authenticate(HttpServletRequest request) {
      final FilterDirector filterDirector = new FilterDirectorImpl();
      filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED);
      filterDirector.setFilterAction(FilterAction.RETURN);

      final String authToken = request.getHeader(CommonHttpHeader.AUTH_TOKEN.toString());
      StringBuilder accountString = new StringBuilder(request.getRequestURI());
      if(includeQueryParams && request.getQueryString()!=null){
          accountString.append("?").append(request.getQueryString());
          
      }
      final ExtractorResult<Object> account = keyedRegexExtractor.extract(accountString.toString());
      CachableUserInfo user = null;

      if ((!StringUtilities.isBlank(authToken) && account != null)) {
         user = checkUserCache(account.getResult(), authToken);

         if (user == null) {
            try {
               user = authenticationService.validateToken(account.getResult(), authToken);
               cacheUserInfo(user);
            } catch (ClientHandlerException ex) {
               LOG.error("Failure communicating with the auth service: " + ex.getMessage(), ex);
               filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
            } catch (Exception ex) {
               LOG.error("Failure in auth: " + ex.getMessage(), ex);
               filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
            }
         }
      }

      Groups groups = null;
      if (user != null) {
         groups = authenticationService.getGroups(user.getUserId());
      }

      final AuthenticationHeaderManager headerManager = new AuthenticationHeaderManager(authToken, user, delegable, filterDirector, account == null ? "" : account.getResult(), groups, request);
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
         cache.storeToken(user.getTenantId(), user, user.tokenTtl().intValue());
      } catch (IOException ex) {
         LOG.warn("Unable to cache user token information: " + user.getUserId() + " Reason: " + ex.getMessage(), ex);
      }
   }

   @Override
   public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {
      return new ResponseHandler(response, getWWWAuthenticateHeaderContents()).handle();
   }
}
