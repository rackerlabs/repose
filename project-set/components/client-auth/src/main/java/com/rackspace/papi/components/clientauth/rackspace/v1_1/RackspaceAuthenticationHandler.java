package com.rackspace.papi.components.clientauth.rackspace.v1_1;

import com.rackspace.papi.components.clientauth.UriMatcher;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;


import com.rackspace.auth.v1_1.AuthenticationServiceClient;
import com.rackspace.auth.v1_1.CachableTokenInfo;
import com.rackspacecloud.docs.auth.api.v1.GroupsList;
import org.slf4j.Logger;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.auth.AuthModule;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientauth.rackspace.config.RackspaceAuth;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

/**
 * @author jhopper
 */
public class RackspaceAuthenticationHandler extends AbstractFilterLogicHandler implements AuthModule {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RackspaceAuthenticationHandler.class);
    private final AuthenticationServiceClient authenticationService;
    private final RackspaceAuth cfg;
    private final KeyedRegexExtractor<String> keyedRegexExtractor;
    private final RackspaceUserInfoCache cache;
    private final UriMatcher uriMatcher;
    private boolean includeQueryParams;

    public RackspaceAuthenticationHandler(RackspaceAuth cfg, AuthenticationServiceClient authServiceClient, KeyedRegexExtractor keyedRegexExtractor, RackspaceUserInfoCache cache, UriMatcher uriMatcher) {
        this.authenticationService = authServiceClient;
        this.cfg = cfg;
        this.keyedRegexExtractor = keyedRegexExtractor;
        this.cache = cache;
        this.uriMatcher = uriMatcher;
        this.includeQueryParams = cfg.isIncludeQueryParams();
    }

    @Override
    public String getWWWAuthenticateHeaderContents() {
        return "RackAuth Realm=\"API Realm\"";
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        FilterDirector filterDirector = new FilterDirectorImpl();
        filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED);
        filterDirector.setFilterAction(FilterAction.RETURN);

        if (uriMatcher.isUriOnWhiteList(request.getRequestURI())) {
            filterDirector.setFilterAction(FilterAction.PASS);
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
        if (includeQueryParams && request.getQueryString() != null) {
            accountString.append("?").append(request.getQueryString());

        }

        final ExtractorResult<String> extractedResult = keyedRegexExtractor.extract(accountString.toString());

        CachableTokenInfo token = null;

        if ((!StringUtilities.isBlank(authToken) && extractedResult != null)) {
            token = checkUserCache(extractedResult.getResult(), authToken);

            if (token == null) {
                try {
                    token = authenticationService.validateToken(extractedResult, authToken);
                    cacheUserInfo(token);
                } catch (Exception ex) {
                    LOG.error("Failure in auth: " + ex.getMessage(), ex);
                    filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
                }
            }
        }

        GroupsList groups = null;
        if (token != null) {
            groups = authenticationService.getGroups(token.getUserId());
        }

        final AuthenticationHeaderManager headerManager = new AuthenticationHeaderManager(token != null, cfg, filterDirector, extractedResult == null ? "" : extractedResult.getResult(), groups, request);
        headerManager.setFilterDirectorValues();

        return filterDirector;
    }

    private CachableTokenInfo checkUserCache(String userId, String token) {
        if (cache == null) {
            return null;
        }

        return cache.getUserToken(userId, token);
    }

    private void cacheUserInfo(CachableTokenInfo user) {
        if (user == null || cache == null) {
            return;
        }

        try {
            cache.storeToken(user.getTenantId(), user, user.safeTokenTtl());
        } catch (IOException ex) {
            LOG.warn("Unable to cache user token information: " + user.getUserId() + " Reason: " + ex.getMessage(), ex);
        }
    }

    @Override
    public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {
        FilterDirector myDirector = new FilterDirectorImpl();
        myDirector.setResponseStatusCode(response.getStatus());
        /// The WWW Authenticate header can be used to communicate to the client
        // (since we are a proxy) how to correctly authenticate itself
        final String wwwAuthenticateHeader = response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString());

        switch (myDirector.getResponseStatus()) {
            // NOTE: We should only mutate the WWW-Authenticate header on a
            // 401 (unauthorized) or 403 (forbidden) response from the origin service
            case UNAUTHORIZED:
            case FORBIDDEN:
                updateHttpResponse(myDirector, wwwAuthenticateHeader);
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

    private void updateHttpResponse(FilterDirector director, String wwwAuthenticateHeader) {
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
    }
}