package com.rackspace.papi.components.clientauth.common;

import com.rackspace.auth.AuthGroup;
import com.rackspace.auth.AuthGroups;
import com.rackspace.auth.AuthServiceException;
import com.rackspace.auth.AuthToken;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author fran
 *
 */
public abstract class AuthenticationHandler extends AbstractFilterLogicHandler {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AuthenticationHandler.class);

   protected abstract AuthToken validateToken(ExtractorResult<String> account, String token);

   protected abstract AuthGroups getGroups(String group);

   protected abstract String getEndpointsBase64(String token, EndpointsConfiguration endpointsConfiguration);

   protected abstract FilterDirector processResponse(ReadableHttpServletResponse response);

   protected abstract void setFilterDirectorValues(String authToken, AuthToken cachableToken, Boolean delegatable, FilterDirector filterDirector, String extractedResult, List<AuthGroup> groups, String endpointsBase64);
   private final boolean delegable;
   private final KeyedRegexExtractor<String> keyedRegexExtractor;
   private final AuthTokenCache cache;
   private final AuthGroupCache grpCache;
   private final EndpointsCache endpointsCache;
   private final UriMatcher uriMatcher;
   private final boolean tenanted;
   private final long groupCacheTtl;
   private final long tokenCacheTtl;
   private final long userCacheTtl;
   private final boolean requestGroups;
   private final AuthUserCache usrCache;
   private final EndpointsConfiguration endpointsConfiguration;

   protected AuthenticationHandler(Configurables configurables, AuthTokenCache cache, AuthGroupCache grpCache, AuthUserCache usrCache, EndpointsCache endpointsCache, UriMatcher uriMatcher) {
      this.delegable = configurables.isDelegable();
      this.keyedRegexExtractor = configurables.getKeyedRegexExtractor();
      this.cache = cache;
      this.grpCache = grpCache;
      this.endpointsCache = endpointsCache;
      this.uriMatcher = uriMatcher;
      this.tenanted = configurables.isTenanted();
      this.groupCacheTtl = configurables.getGroupCacheTtl();
      this.tokenCacheTtl = configurables.getTokenCacheTtl();
      this.userCacheTtl = configurables.getUserCacheTtl();
      this.requestGroups = configurables.isRequestGroups();
      this.usrCache = usrCache;
      this.endpointsConfiguration = configurables.getEndpointsConfiguration();
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
               LOG.error("Failure in Auth-N: " + ex.getMessage());
               filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
            } catch (IllegalArgumentException ex) {
               LOG.error("Failure in Auth-N: " + ex.getMessage());
               filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
            } catch (Exception ex) {
               LOG.error("Failure in auth: " + ex.getMessage(), ex);
               filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
            }
         }
      }

       List<AuthGroup> groups = getAuthGroups(token);

       //getting the encoded endpoints to pass into the header, if the endpoints config is not null
       String endpointsInBase64 = null;
       if (endpointsConfiguration != null){
           endpointsInBase64 = getEndpointsInBase64(token);
       }

      setFilterDirectorValues(authToken, token, delegable, filterDirector, account == null ? "" : account.getResult(),
              groups, endpointsInBase64);

      return filterDirector;
   }

   //check for null, check for it already in cache
   private String getEndpointsInBase64(AuthToken token) {
      String tokenId = null;

      if (token != null) {
         tokenId = token.getTokenId();
      }

      String endpoints = checkEndpointsCache(tokenId);

      //if endpoints are not already in the cache then make a call for them and cache what comes back
      if (endpoints == null) {
         endpoints = getEndpointsBase64(tokenId, endpointsConfiguration);
         cacheEndpoints(tokenId, endpoints);
      }

      return endpoints;
   }

   //cache check for endpoints
   private String checkEndpointsCache(String token) {
      if (endpointsCache == null) {
         return null;
      }

      return endpointsCache.getEndpoints(token);
   }

   private List<AuthGroup> getAuthGroups(AuthToken token) {
      if (token != null && requestGroups) {

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

      return keyedRegexExtractor.extract(accountString.toString());
   }

   private boolean allowAccount(ExtractorResult<String> account) {

      if (tenanted) {
         return account != null;
      } else {
         return true;
      }
   }

   /*
    * New caching strategy:
    * If running in tenanted mode we will look into the user cache for list of tokens, if passed token is present we will look to the token cache and return the Auth
    * token object. If running in non-tenanted mode we
    */
   private AuthToken checkToken(ExtractorResult<String> account, String authToken) {

      AuthToken token = checkTokenCache(authToken);
      if (token != null) {
         if (tenanted) {

            return StringUtilities.nullSafeEqualsIgnoreCase(account.getResult(), token.getTenantId()) ? token : null;
         }
      }
      return token;


   }

   private AuthToken checkTokenCache(String token) {
      if (cache == null) {
         return null;
      }
      return cache.getUserToken(token);
   }

   /*
    * New caching strategy:
    * Tokens (TokenId) will be mapped to AuthToken object.
    * userId will be mapped to List of TokenIds
    */
   private void cacheUserInfo(AuthToken user) {

      if (user == null || cache == null) {
         return;
      }

      String userKey = user.getUserId();
      String tokenKey = user.getTokenId();

      //Adds auth token object to cache.
      try {

         long userTokenTtl = user.tokenTtl().intValue();
          if (userTokenTtl > Integer.MAX_VALUE || userTokenTtl < 0) {
              LOG.warn("Token TTL (" + user.getTokenId() + ") exceeds max expiration, setting to default max expiration");
              userTokenTtl = Integer.MAX_VALUE;
          }

         long ttl = tokenCacheTtl > 0 ? Math.min(tokenCacheTtl, userTokenTtl) : userTokenTtl;

         cache.storeToken(tokenKey, user, Long.valueOf(ttl).intValue());
      } catch (IOException ex) {
         LOG.warn("Unable to cache user token information: " + user.getUserId() + " Reason: " + ex.getMessage(), ex);
      }

      Set<String> userTokenList = getUserTokenList(userKey);

      userTokenList.add(tokenKey);

      try {
         long ttl = userCacheTtl;
         usrCache.storeUserTokenList(userKey, userTokenList, Long.valueOf(ttl).intValue());
      } catch (IOException ex) {
         LOG.warn("Unable to cache user token information: " + user.getUserId() + " Reason: " + ex.getMessage(), ex);
      }
      //TODO: Search cache for user object.
      // Present: Add token to user token list
      // Not Present: Add token to new user token list and add new user token list to cache
   }

   private Set<String> getUserTokenList(String userKey) {

      Set<String> userTokenList = usrCache.getUserTokenList(userKey);


      if (userTokenList == null) {
         userTokenList = new HashSet<String>();
      }

      return userTokenList;
   }

   private String getGroupCacheKey(AuthToken token) {
      return token.getTokenId();
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

   //store endpoints in cache
   private void cacheEndpoints(String token, String endpoints) {
      if (token == null || endpointsCache == null) {
         return;
      }

      try {
         endpointsCache.storeEndpoints(token, endpoints, safeEndpointsTtl());
      } catch (IOException ex) {
         LOG.warn("Unable to cache endpoints information: " + token + " Reason: " + ex.getMessage(), ex);
      }
   }

   private int safeGroupTtl() {
      final Long grpTtl = this.groupCacheTtl;

      if (grpTtl >= Integer.MAX_VALUE) {
         return Integer.MAX_VALUE;
      }

      return grpTtl.intValue();
   }

   //get the ttl but prevent bad integers
    private Integer safeEndpointsTtl() {
        final Long endpointsTtl;

        if (endpointsConfiguration != null) {
            endpointsTtl = endpointsConfiguration.getCacheTimeout();
        } else {
            return null;
        }

      if (endpointsTtl >= Integer.MAX_VALUE) {
         return Integer.MAX_VALUE;
      }

      return endpointsTtl.intValue();
   }
}
