package com.rackspace.papi.components.clientauth.rackspace.v1_1;

import com.rackspace.auth.AuthGroup;
import com.rackspace.auth.AuthToken;
import com.rackspace.auth.rackspace.AuthenticationService;
import com.rackspace.papi.components.clientauth.common.AuthModule;
import com.rackspace.papi.components.clientauth.common.UriMatcher;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;

import org.slf4j.Logger;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientauth.rackspace.config.RackspaceAuth;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * @author jhopper
 */
public class RackspaceAuthenticationHandler extends AbstractFilterLogicHandler implements AuthModule {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RackspaceAuthenticationHandler.class);
    private final AuthenticationService authenticationService;
    private final RackspaceAuth cfg;
    private final KeyedRegexExtractor<String> keyedRegexExtractor;
    private final RackspaceUserInfoCache cache;
    private final UriMatcher uriMatcher;
    private boolean includeQueryParams;

    public RackspaceAuthenticationHandler(RackspaceAuth cfg, AuthenticationService authServiceClient, KeyedRegexExtractor keyedRegexExtractor, RackspaceUserInfoCache cache, UriMatcher uriMatcher) {
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

        AuthToken token = null;

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

        List<AuthGroup> groups = new ArrayList<AuthGroup>();
        if (token != null) {
            groups = authenticationService.getGroups(token.getUserId());
        }

        final AuthenticationHeaderManager headerManager = new AuthenticationHeaderManager(token != null, cfg, filterDirector, extractedResult == null ? "" : extractedResult.getResult(), groups);
        headerManager.setFilterDirectorValues();

        return filterDirector;
    }

    private AuthToken checkUserCache(String userId, String token) {
        if (cache == null) {
            return null;
        }

        return cache.getUserToken(userId, token);
    }

    private void cacheUserInfo(AuthToken user) {
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
        return new ResponseHandler(response, getWWWAuthenticateHeaderContents()).handle();
    }
}