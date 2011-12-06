package com.rackspace.papi.components.clientauth.rackspace.v1_1;

import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;


import com.rackspace.auth.v1_1.Account;
import com.rackspace.auth.v1_1.AuthenticationServiceClient;
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

import javax.servlet.http.HttpServletRequest;

/**
 * @author jhopper
 */
public class RackspaceAuthenticationHandler extends AbstractFilterLogicHandler implements AuthModule {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RackspaceAuthenticationHandler.class);
    private final AuthenticationServiceClient authenticationService;
    private final RackspaceAuth cfg;
    private final AccountUsernameExtractor accountUsernameExtractor;

    public RackspaceAuthenticationHandler(RackspaceAuth cfg, AuthenticationServiceClient authServiceClient) {
        this.authenticationService = authServiceClient;
        this.cfg = cfg;
        this.accountUsernameExtractor = new AccountUsernameExtractor(cfg.getAccountMapping());
    }

    @Override
    public String getWWWAuthenticateHeaderContents() {
        return "RackAuth Realm=\"API Realm\"";
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        return authenticate(request);
    }

    @Override
    public FilterDirector authenticate(HttpServletRequest request) {
        final FilterDirector filterDirector = new FilterDirectorImpl();
        filterDirector.setResponseStatus(HttpStatusCode.UNAUTHORIZED);
        filterDirector.setFilterAction(FilterAction.RETURN);

        final String authToken = request.getHeader(CommonHttpHeader.AUTH_TOKEN.getHeaderKey());
        final Account acct = accountUsernameExtractor.extract(request.getRequestURL().toString());

        boolean validToken = false;

        if ((!StringUtilities.isBlank(authToken) && acct != null)) {
            try {
                validToken = authenticationService.validateToken(acct, authToken);
            } catch (Exception ex) {
                LOG.error("Failure in auth: " + ex.getMessage(), ex);
                filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
            }


        }

        String[] groups = null;
        if (validToken) {
            groups = getGroupsListIds(acct.getUsername());
        }

        final AuthenticationHeaderManager headerManager = new AuthenticationHeaderManager(validToken, cfg, filterDirector, acct == null ? "" : acct.getUsername(), groups);
        headerManager.setFilterDirectorValues();

        return filterDirector;
    }

    private String[] getGroupsListIds(String accountUsername) {
        GroupsList groups = authenticationService.getGroups(accountUsername);
        int groupCount = groups.getGroup().size();

        String[] groupsArray = new String[groupCount];

        for (int i = 0; i < groupCount; i++) {
            groupsArray[i] = groups.getGroup().get(i).getId();
        }

        return groupsArray;
    }

    @Override
    public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {
        /// The WWW Authenticate header can be used to communicate to the client
        // (since we are a proxy) how to correctly authenticate itself
        final String wwwAuthenticateHeader = response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.getHeaderKey());

        switch (HttpStatusCode.fromInt(response.getStatus())) {
            // NOTE: We should only mutate the WWW-Authenticate header on a
            // 401 (unauthorized) or 403 (forbidden) response from the origin service
            case UNAUTHORIZED:
            case FORBIDDEN:
                updateHttpResponse(response, wwwAuthenticateHeader);
                break;
        }

        // TODO: Do we need to return a valid FilterDirector here?
        return null;
    }

    private void updateHttpResponse(ReadableHttpServletResponse httpResponse, String wwwAuthenticateHeader) {

        // If in the case that the origin service supports delegated authentication
        // we should then communicate to the client how to authenticate with us
        if (!StringUtilities.isBlank(wwwAuthenticateHeader) && wwwAuthenticateHeader.contains("Delegated")) {
            final String replacementWwwAuthenticateHeader = getWWWAuthenticateHeaderContents();
            httpResponse.setHeader(CommonHttpHeader.WWW_AUTHENTICATE.getHeaderKey(), replacementWwwAuthenticateHeader);
        } else {
            // In the case where authentication has failed and we did not receive
            // a delegated WWW-Authenticate header, this means that our own authentication
            // with the origin service has failed and must then be communicated as
            // a 500 (internal server error) to the client
            httpResponse.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue());
        }
    }
}