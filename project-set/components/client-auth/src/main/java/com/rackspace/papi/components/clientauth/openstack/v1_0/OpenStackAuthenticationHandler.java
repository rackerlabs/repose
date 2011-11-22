package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.openstack.ids.Account;
import com.rackspace.auth.openstack.ids.CachableTokenInfo;
import com.rackspace.auth.openstack.ids.OpenStackAuthenticationService;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;

import com.rackspace.papi.auth.AuthModule;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientauth.openstack.config.OpenstackAuth;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * @author fran
 */
public class OpenStackAuthenticationHandler extends AbstractFilterLogicHandler implements AuthModule {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(OpenStackAuthenticationHandler.class);
    private final OpenStackAuthenticationService authenticationService;
    private final AccountUsernameExtractor accountUsernameExtractor;
    private boolean delegatable;
    private final String authServiceUri;

    public OpenStackAuthenticationHandler(OpenstackAuth cfg, OpenStackAuthenticationService serviceClient) {
        this.authenticationService = serviceClient;
        this.accountUsernameExtractor = new AccountUsernameExtractor(cfg.getClientMapping());
        delegatable = cfg.isDelegatable();
        this.authServiceUri = cfg.getIdentityService().getUri();
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

        final String authToken = request.getHeader(CommonHttpHeader.AUTH_TOKEN.getHeaderKey());
        final Account account = accountUsernameExtractor.extract(request.getRequestURI());
        CachableTokenInfo cachableTokenInfo = null;

        if ((!StringUtilities.isBlank(authToken) && account != null)) {
            try {
                cachableTokenInfo = authenticationService.validateToken(account.getUsername(), authToken);
            } catch (Exception ex) {
                LOG.error("Failure in auth: " + ex.getMessage(), ex);
                filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
            }
        }

        Groups groups = null;
        if (cachableTokenInfo != null) {
            groups = authenticationService.getGroups(cachableTokenInfo.getUserId());
        }

        final AuthenticationHeaderManager headerManager = new AuthenticationHeaderManager(cachableTokenInfo, delegatable, filterDirector, account == null ? "" : account.getUsername(), groups);
        headerManager.setFilterDirectorValues();

        return filterDirector;
    }

    @Override
    public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {
        FilterDirector myDirector = new FilterDirectorImpl();

        /// The WWW Authenticate header can be used to communicate to the client
        // (since we are a proxy) how to correctly authenticate itself
        final String wwwAuthenticateHeader = response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.getHeaderKey());

        switch (HttpStatusCode.fromInt(response.getStatus())) {
            // NOTE: We should only mutate the WWW-Authenticate header on a
            // 401 (unauthorized) or 403 (forbidden) response from the origin service
            case UNAUTHORIZED:
            case FORBIDDEN:
                myDirector = updateHttpResponse(myDirector, wwwAuthenticateHeader);
                break;
        }
        
        return myDirector;
    }

    private FilterDirector updateHttpResponse(FilterDirector director, String wwwAuthenticateHeader) {
        final FilterDirector myDirector = new FilterDirectorImpl(director);

        // If in the case that the origin service supports delegated authentication
        // we should then communicate to the client how to authenticate with us
        if (!StringUtilities.isBlank(wwwAuthenticateHeader) && wwwAuthenticateHeader.contains("Delegated")) {
            final String replacementWwwAuthenticateHeader = getWWWAuthenticateHeaderContents();
            myDirector.responseHeaderManager().putHeader(CommonHttpHeader.WWW_AUTHENTICATE.getHeaderKey(), replacementWwwAuthenticateHeader);
        } else {
            // In the case where authentication has failed and we did not receive
            // a delegated WWW-Authenticate header, this means that our own authentication
            // with the origin service has failed and must then be communicated as
            // a 500 (internal server error) to the client
            myDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
        }
        
        return myDirector;
    }
}
