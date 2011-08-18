package com.rackspace.papi.components.clientauth.rackspace;

import com.rackspace.auth.v1_1.Account;
import com.rackspace.auth.v1_1.AuthServiceClient;
import com.rackspace.auth.v1_1.AuthServiceException;
import com.rackspace.auth.v1_1.AuthenticationResponse;
import com.rackspace.auth.v1_1.TokenCache;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspacecloud.docs.auth.api.v1.GroupsList;
import org.slf4j.Logger;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.auth.AuthModule;
import com.rackspace.papi.components.clientauth.rackspace.config.AccountMapping;
import com.rackspace.papi.components.clientauth.rackspace.config.RackspaceAuth;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 *
 * @author jhopper
 */
public class RackspaceAuthModule implements AuthModule {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RackspaceAuthModule.class);
    private final TokenCache authTokenCache;
    private final AuthServiceClient authService;
    private final RackspaceAuth cfg;
    private final Map<AccountMapping, Pattern> compiledAccountMappingRegexes;

    public RackspaceAuthModule(CacheManager cache, RackspaceAuth cfg) {
        this.authTokenCache = new TokenCache(cache);
        this.authService = new AuthServiceClient(cfg.getAuthenticationServer().getUri(), cfg.getAuthenticationServer().getUsername(), cfg.getAuthenticationServer().getPassword());

        this.cfg = cfg;

        compiledAccountMappingRegexes = new HashMap<AccountMapping, Pattern>();

        compileRegexCache();
    }

    private void compileRegexCache() {
        for (AccountMapping mapping : cfg.getAccountMapping()) {
            compiledAccountMappingRegexes.put(mapping, Pattern.compile(mapping.getIdRegex()));
        }
    }

    private Account extractAccountFromUri(String uri) {
        for (Map.Entry<AccountMapping, Pattern> entry : compiledAccountMappingRegexes.entrySet()) {
            final Matcher accountIdMatcher = entry.getValue().matcher(uri);

            if (accountIdMatcher.find()) {
                return new Account(entry.getKey().getType().name(), accountIdMatcher.group(accountIdMatcher.groupCount()));
            }
        }

        return null;
    }

    @Override
    public String getWWWAuthenticateHeaderContents() {
        return "RackAuth Realm=\"API Realm\"";
    }

    @Override
    public FilterDirector authenticate(HttpServletRequest request) {
        final FilterDirector filterDirector = new FilterDirectorImpl();
        filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED);
        filterDirector.setFilterAction(FilterAction.USE_MESSAGE_SERVICE);

        final String authToken = request.getHeader(CommonHttpHeader.AUTH_TOKEN.headerKey());
        final Account acct = extractAccountFromUri(request.getRequestURL().toString());

        if (!StringUtilities.isBlank(authToken)) {
            if (acct != null) {
                try {
                    validateToken(acct, authToken, filterDirector);
                } catch (Exception ex) {
                    LOG.error("Failure in auth: " + ex.getMessage(), ex);
                    filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
                }
            }
        } else if (cfg.isDelegatable()) {
            filterDirector.setFilterAction(FilterAction.PASS);

            filterDirector.requestHeaderManager().putHeader(CommonHttpHeader.EXTENDED_AUTHORIZATION.headerKey(), "proxy " + acct.getId());
            filterDirector.requestHeaderManager().putHeader(CommonHttpHeader.IDENTITY_STATUS.headerKey(), "Indeterminate");
        }

        return filterDirector;
    }

    private void validateToken(Account acct, String authToken, FilterDirector director) throws AuthServiceException {
        boolean authenticated = tokenMatchesCachedTokenForAccount(acct.getId(), authToken);

        if (!authenticated) {
            final AuthenticationResponse authServiceResponse = authService.validateToken(acct, authToken);

            if (authServiceResponse.authenticated()) {
                //TODO:REVIEW: I had this deleted...was re-introduced in merge...ensure we really need it
                director.setResponseStatus(HttpStatusCode.fromInt(authServiceResponse.getResponseCode()));
                cacheUserAuthToken(acct.getId(), authServiceResponse.getTtl(), authServiceResponse.getAuthToken());

                authenticated = true;
            }
        }

        // TODO: Update authentication failure logic when service supports delegation
        
        // If a service supports delegation then we still need to pass the request.
        // This logic should be reflected in this code but it isn't at current.
        // See: cfg.isDelegatable()
        
        // NOTE: Regarding the identity status header below...
        // Regardless of whether or not we were able to authenticate the user
        // we need to communicate this to the underlying service if and only if
        // they support delegation
        director.requestHeaderManager().putHeader(CommonHttpHeader.IDENTITY_STATUS.headerKey(), authenticated ? "Confirmed" : "Indeterminate");
        
        if (authenticated) {
            director.requestHeaderManager().putHeader(CommonHttpHeader.EXTENDED_AUTHORIZATION.headerKey(), "proxy " + acct.getId());

            // TODO: annotate request with group information from auth

            director.requestHeaderManager().putHeader(PowerApiHeader.GROUPS.headerKey(), getGroupsListIds(acct.getId()));
            director.requestHeaderManager().putHeader(PowerApiHeader.USER.headerKey(), acct.getId());
            director.setFilterAction(FilterAction.PASS);
        }
    }

    private String[] getGroupsListIds(String acctId) {
        GroupsList groups = authService.getGroups(acctId);
        int groupCount = groups.getGroup().size();

        String[] groupsArray = new String[groupCount];

        for (int i = 0; i < groupCount; i++) {
            groupsArray[i] = groups.getGroup().get(i).getId();
        }

        return groupsArray;
    }

    public void cacheUserAuthToken(String accountId, int ttl, String authToken) {
        final Element newCacheElement = new Element(accountId, authToken);
        newCacheElement.setTimeToLive(ttl);

        authTokenCache.put(newCacheElement);
    }

    public boolean tokenMatchesCachedTokenForAccount(String accountId, String headerAuthToken) {
        final String cachedAuthToken = queryCacheForAuthToken(accountId);

        return cachedAuthToken != null && cachedAuthToken.equals(headerAuthToken);
    }

    public String queryCacheForAuthToken(String accountId) {
        final Element cachedTokenElement = authTokenCache.get(accountId);

        return cachedTokenElement != null && !cachedTokenElement.isExpired() ? (String) cachedTokenElement.getValue() : null;
    }
}
