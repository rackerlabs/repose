package com.rackspace.papi.components.clientauth.common;

import com.rackspace.auth.AuthGroup;
import com.rackspace.auth.AuthGroups;
import com.rackspace.auth.AuthToken;
import com.rackspace.auth.AuthServiceException;
import com.rackspace.papi.commons.util.StringUriUtilities;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.sun.jersey.api.client.ClientHandlerException;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fran
 */
public abstract class AuthenticationHandler extends AbstractFilterLogicHandler {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AuthenticationHandler.class);

   protected abstract AuthToken validateToken(ExtractorResult<String> account, String token);

   protected abstract AuthGroups getGroups(String group);

   protected abstract FilterDirector processResponse(ReadableHttpServletResponse response);

   protected abstract void setFilterDirectorValues(String authToken, AuthToken cachableToken, Boolean delegatable, FilterDirector filterDirector, String extractedResult, List<AuthGroup> groups);
   private final boolean delegable;
   private final KeyedRegexExtractor<String> keyedRegexExtractor;
   private final AuthTokenCache cache;
   private final AuthGroupCache grpCache;
   private final UriMatcher uriMatcher;
   private final boolean includeQueryParams, tenanted;
   private final long groupCacheTtl;
   private final long userCacheTtl;

   protected AuthenticationHandler(Configurables configurables, AuthTokenCache cache, AuthGroupCache grpCache, UriMatcher uriMatcher) {
      this.delegable = configurables.isDelegable();
      this.keyedRegexExtractor = configurables.getKeyedRegexExtractor();
      this.cache = cache;
      this.grpCache = grpCache;
      this.uriMatcher = uriMatcher;
      this.includeQueryParams = configurables.isIncludeQueryParams();
      this.tenanted = configurables.isTenanted();
      this.groupCacheTtl = configurables.getGroupCacheTtl();
      this.userCacheTtl = configurables.getUserCacheTtl();
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      FilterDirector filterDirector = new FilterDirectorImpl();
      filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED);
      filterDirector.setFilterAction(FilterAction.RETURN);

      final String uri = request.getRequestURI();
      LOG.debug("Uri is " + uri);
      if (uriMatcher.isUriOnWhiteList(uri)) {
         filterDirector.setFilterAction(FilterAction.PASS);
         LOG.debug("Uri is on whitelist!  Letting request pass through.");
      } else {
         filterDirector = this.authenticate(request);
      }

      return filterDirector;
   }

   @Override
   public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {
      return processResponse(response);
   }

   private FilterDirector authenticate(HttpServletRequest request) {
      final FilterDirector filterDirector = new FilterDirectorImpl();
      filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED);
      filterDirector.setFilterAction(FilterAction.RETURN);

      final String authToken = request.getHeader(CommonHttpHeader.AUTH_TOKEN.toString());
      ExtractorResult<String> account = null;
      AuthToken token = null;


      if (tenanted) {
         account = extractAccountIdentification(request);
      }

      final boolean allow = allowAccount(account);

      if ((!StringUtilities.isBlank(authToken) && allow)) {
         token = checkToken(account, authToken);

         if (token == null) {
            try {
               token = validateToken(account, StringUriUtilities.encodeUri(authToken));
               cacheUserInfo(token);
            } catch (ClientHandlerException ex) {
               LOG.error("Failure communicating with the auth service: " + ex.getMessage(), ex);
               filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
            } catch (AuthServiceException ex) {
               LOG.error("Failure in auth: " + ex.getMessage());
               filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
            } catch (Exception ex) {
               LOG.error("Failure in auth: " + ex.getMessage(), ex);
               filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
            }
         }
      }

      List<AuthGroup> groups = getAuthGroups(token);

      setFilterDirectorValues(authToken, token, delegable, filterDirector, account == null ? "" : account.getResult(), groups);

      return filterDirector;
   }

   private List<AuthGroup> getAuthGroups(AuthToken token) {
      if (token != null) {

         AuthGroups authGroups = checkGroupCache(token);

         if (authGroups == null) {
            try {
               authGroups = getGroups(token.getUserId());
               cacheGroupInfo(token, authGroups);
            } catch (ClientHandlerException ex) {
               LOG.error("Failure communicating with the auth service when retrieving groups: " + ex.getMessage(), ex);
               LOG.error("X-PP-Groups will not be set.");
            } catch (Exception ex) {
               LOG.error("Failure in auth when retrieving groups: " + ex.getMessage(), ex);
               LOG.error("X-PP-Groups will not be set.");
            }
         }

         if (authGroups != null && authGroups.getGroups() != null) {
            return authGroups.getGroups();
         }
      }
      return new ArrayList<AuthGroup>();

   }

   private ExtractorResult<String> extractAccountIdentification(HttpServletRequest request) {
      StringBuilder accountString = new StringBuilder(request.getRequestURI());
      if (includeQueryParams && request.getQueryString() != null) {
         accountString.append("?").append(request.getQueryString());

      }

      return keyedRegexExtractor.extract(accountString.toString());
   }

   private boolean allowAccount(ExtractorResult<String> account) {

      if (tenanted) {
         return account != null;
      } else {
         return true;
      }
   }

   private AuthToken checkToken(ExtractorResult<String> account, String authToken) {
      if (tenanted) {
         return checkUserCache(account.getResult(), authToken);
      } else {
         return checkUserCache("", authToken);
      }
   }

   private AuthToken checkUserCache(String tenantId, String token) {
      if (cache == null) {
         return null;
      }

      String key;
      if (StringUtilities.isNotBlank(tenantId)) {
         key = tenantId + ":" + token;
      } else {
         key = token;
      }

      return cache.getUserToken(key, token);
   }

   private void cacheUserInfo(AuthToken user) {
      if (user == null || cache == null) {
         return;
      }
      String key;
      if (tenanted) {
         key = user.getTenantId() + ":" + user.getTokenId();
      } else {
         key = user.getTokenId();
      }

      try {
         long ttl = userCacheTtl > 0 ? Math.min(userCacheTtl, user.tokenTtl().intValue()) : user.tokenTtl().intValue();
         cache.storeToken(key, user, Long.valueOf(ttl).intValue());
      } catch (IOException ex) {
         LOG.warn("Unable to cache user token information: " + user.getUserId() + " Reason: " + ex.getMessage(), ex);
      }
   }

   private String getGroupCacheKey(AuthToken token) {
      return token.getTenantId() + ":" + token.getTokenId();
   }

   private AuthGroups checkGroupCache(AuthToken token) {
      if (grpCache == null) {
         return null;
      }

      return grpCache.getUserGroup(getGroupCacheKey(token));
   }

   private void cacheGroupInfo(AuthToken token, AuthGroups groups) {
      if (groups == null || grpCache == null) {
         return;
      }

      try {
         grpCache.storeGroups(getGroupCacheKey(token), groups, safeGroupTtl());
      } catch (IOException ex) {
         LOG.warn("Unable to cache user group information: " + token + " Reason: " + ex.getMessage(), ex);
      }
   }

   private int safeGroupTtl() {
      final Long grpTtl = this.groupCacheTtl;

      if (grpTtl >= Integer.MAX_VALUE) {
         return Integer.MAX_VALUE;
      }

      return grpTtl.intValue();
   }
}
