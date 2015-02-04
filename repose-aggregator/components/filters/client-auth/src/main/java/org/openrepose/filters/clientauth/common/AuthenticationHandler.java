package org.openrepose.filters.clientauth.common;

import org.openrepose.common.auth.AuthGroup;
import org.openrepose.common.auth.AuthGroups;
import org.openrepose.common.auth.AuthServiceException;
import org.openrepose.common.auth.AuthToken;
import org.openrepose.commons.utils.StringUriUtilities;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.regex.ExtractorResult;
import org.openrepose.commons.utils.regex.KeyedRegexExtractor;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.services.serviceclient.akka.AkkaServiceClientException;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * @author fran
 */
public abstract class AuthenticationHandler extends AbstractFilterLogicHandler {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AuthenticationHandler.class);

    protected abstract AuthToken validateToken(ExtractorResult<String> account, String token) throws AuthServiceException;

    protected abstract AuthGroups getGroups(String group) throws AuthServiceException;

    protected abstract String getEndpointsBase64(String token, EndpointsConfiguration endpointsConfiguration) throws AuthServiceException;

    protected abstract FilterDirector processResponse(ReadableHttpServletResponse response);

    protected abstract void setFilterDirectorValues(String authToken, AuthToken cachableToken, Boolean delegatable,
                                                    double delegableQuality, String delegationMessage,
                                                    FilterDirector filterDirector, String extractedResult,
                                                    List<AuthGroup> groups, String endpointsBase64, boolean tenanted,
                                                    boolean sendAllTenantIds, boolean sendTenantIdQuality);

    private final boolean delegable;
    private final double delegableQuality;
    private final KeyedRegexExtractor<String> keyedRegexExtractor;
    private final AuthTokenCache cache;
    private final AuthGroupCache grpCache;
    private final EndpointsCache endpointsCache;
    private final UriMatcher uriMatcher;
    private final boolean tenanted;
    private final long groupCacheTtl;
    private final long tokenCacheTtl;
    private final long userCacheTtl;
    private final Random offsetGenerator;
    private int cacheOffset;
    private final boolean requestGroups;
    private final AuthUserCache usrCache;
    private final EndpointsConfiguration endpointsConfiguration;
    private static final String REASON = " Reason: ";
    private static final String FAILURE_AUTH_N = "Failure in Auth-N filter.";
    private final boolean sendAllTenantIds;
    private final boolean sendTenantIdQuality;

    protected static final ThreadLocal<String> delegationMessage = new ThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return FAILURE_AUTH_N;
        }
    };

    protected AuthenticationHandler(Configurables configurables, AuthTokenCache cache, AuthGroupCache grpCache, AuthUserCache usrCache, EndpointsCache endpointsCache, UriMatcher uriMatcher) {
        this.delegable = configurables.isDelegable();
        this.delegableQuality = configurables.getDelegableQuality();
        this.keyedRegexExtractor = configurables.getKeyedRegexExtractor();
        this.cache = cache;
        this.grpCache = grpCache;
        this.endpointsCache = endpointsCache;
        this.uriMatcher = uriMatcher;
        this.tenanted = configurables.isTenanted();
        this.groupCacheTtl = configurables.getGroupCacheTtl();
        this.tokenCacheTtl = configurables.getTokenCacheTtl();
        this.userCacheTtl = configurables.getUserCacheTtl();
        this.offsetGenerator = new Random();
        this.cacheOffset = configurables.getCacheOffset();
        this.requestGroups = configurables.isRequestGroups();
        this.usrCache = usrCache;
        this.endpointsConfiguration = configurables.getEndpointsConfiguration();
        this.sendAllTenantIds = configurables.sendingAllTenantIds();
        this.sendTenantIdQuality = configurables.sendTenantIdQuality();
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        FilterDirector filterDirector = new FilterDirectorImpl();
        filterDirector.setResponseStatusCode(HttpServletResponse.SC_UNAUTHORIZED);
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
        filterDirector.setResponseStatusCode(HttpServletResponse.SC_UNAUTHORIZED);
        filterDirector.setFilterAction(FilterAction.RETURN);
        int offset = getCacheOffset();

        final String authToken = request.getHeader(CommonHttpHeader.AUTH_TOKEN.toString());
        ExtractorResult<String> account = null;
        AuthToken token = null;

        if (tenanted) {
            account = extractAccountIdentification(request);
        }

        final boolean allow = allowAccount(account);

        String endpointsInBase64 = "";
        List<AuthGroup> groups = new ArrayList<AuthGroup>();
        try {
            if (!StringUtilities.isBlank(authToken) && allow) {
                token = checkToken(account, authToken);

                if (token == null) {
                    token = validateToken(account, StringUriUtilities.encodeUri(authToken));
                    cacheUserInfo(token, offset);
                }
            }

            if (token != null) {
                groups = getAuthGroups(token, offset);

                //getting the encoded endpoints to pass into the header, if the endpoints config is not null
                if (endpointsConfiguration != null) {
                    endpointsInBase64 = getEndpointsInBase64(token);
                }
            }
        } catch (AuthServiceException ex) {
            LOG.error(FAILURE_AUTH_N + REASON + ex.getMessage());
            LOG.trace("", ex);
            if(ex.getCause() instanceof AkkaServiceClientException && ex.getCause().getCause() instanceof TimeoutException) {
                filterDirector.setResponseStatusCode(HttpServletResponse.SC_GATEWAY_TIMEOUT);
            } else {
                filterDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            delegationMessage.set(FAILURE_AUTH_N);
        } catch (Exception ex) {
            LOG.error(FAILURE_AUTH_N, ex);
            filterDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            delegationMessage.set(FAILURE_AUTH_N + REASON + ex.getMessage());
        }

        setFilterDirectorValues(authToken, token, delegable, delegableQuality, delegationMessage.get(), filterDirector,
                account == null ? "" : account.getResult(), groups, endpointsInBase64, tenanted, sendAllTenantIds,
                sendTenantIdQuality);

        delegationMessage.remove();

        return filterDirector;
    }

    //check for null, check for it already in cache
    private String getEndpointsInBase64(AuthToken token) throws AuthServiceException {
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

    private List<AuthGroup> getAuthGroups(AuthToken token, int offset) throws AuthServiceException {
        if (token != null && requestGroups) {

            AuthGroups authGroups = checkGroupCache(token);

            if (authGroups == null) {

                authGroups = getGroups(token.getUserId());
                cacheGroupInfo(token, authGroups, offset);
            }

            if (authGroups != null && authGroups.getGroups() != null) {
                return authGroups.getGroups();
            }
        }
        return new ArrayList<>();

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
    private void cacheUserInfo(AuthToken user, int offset) {

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

            long ttl = tokenCacheTtl > 0 ? Math.min(getMaxTTL(tokenCacheTtl + offset), userTokenTtl) : userTokenTtl;
            LOG.debug("Caching token for " + user.getTenantId() + " with a TTL of " + ttl);
            cache.storeToken(tokenKey, user, Long.valueOf(ttl).intValue());
        } catch (IOException ex) {
            LOG.warn("Unable to cache user token information: " + user.getUserId() + REASON, ex);
        }

        Set<String> userTokenList = getUserTokenList(userKey);

        userTokenList.add(tokenKey);

        try {
            long ttl = userCacheTtl;
            usrCache.storeUserTokenList(userKey, userTokenList, Long.valueOf(ttl).intValue());
        } catch (IOException ex) {
            LOG.warn("Unable to cache user token information: " + user.getUserId() + REASON, ex);
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

    private void cacheGroupInfo(AuthToken token, AuthGroups groups, int offset) {
        if (groups == null || grpCache == null) {
            return;
        }

        try {
            grpCache.storeGroups(getGroupCacheKey(token), groups, safeGroupTtl(offset));
        } catch (IOException ex) {
            LOG.warn("Unable to cache user group information: " + token + REASON, ex);
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
            LOG.warn("Unable to cache endpoints information: " + token + REASON, ex);
        }
    }

    private int safeGroupTtl(int offset) {
        final Long grpTtl = this.groupCacheTtl + offset;

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

    public int getCacheOffset() {
        return cacheOffset == 0 ? 0 : offsetGenerator.nextInt(cacheOffset * 2) - cacheOffset;

    }

    public long getMaxTTL(long ttl) {
        return Math.min(ttl, Integer.MAX_VALUE);
    }
}
