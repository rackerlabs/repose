/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.clientauth.openstack;

import org.apache.commons.lang3.StringUtils;
import org.openrepose.common.auth.AuthGroup;
import org.openrepose.common.auth.AuthGroups;
import org.openrepose.common.auth.AuthServiceException;
import org.openrepose.common.auth.AuthToken;
import org.openrepose.common.auth.openstack.AuthenticationService;
import org.openrepose.common.auth.openstack.AuthenticationServiceClient;
import org.openrepose.common.auth.openstack.OpenStackToken;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.regex.ExtractorResult;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.filters.clientauth.common.*;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author fran
 */
@Deprecated
public class OpenStackAuthenticationHandler extends AuthenticationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(OpenStackAuthenticationHandler.class);
    private static final String WWW_AUTH_PREFIX = "Keystone uri=";
    private static final String DELEGATED = "Delegated";
    private final String wwwAuthHeaderContents;
    private final AuthenticationService authenticationService;
    private final List<String> serviceAdminRoles;
    private final List<String> ignoreTenantRoles;
    private boolean delegatingMode;

    public OpenStackAuthenticationHandler(
            Configurables cfg,
            AuthenticationService serviceClient,
            AuthTokenCache cache,
            AuthGroupCache grpCache,
            AuthUserCache usrCache,
            EndpointsCache endpointsCache,
            UriMatcher uriMatcher) {

        super(cfg, cache, grpCache, usrCache, endpointsCache, uriMatcher);
        this.authenticationService = serviceClient;
        this.wwwAuthHeaderContents = WWW_AUTH_PREFIX + cfg.getAuthServiceUri();
        this.serviceAdminRoles = cfg.getServiceAdminRoles();
        this.ignoreTenantRoles = cfg.getIgnoreTenantRoles();
        this.delegatingMode = cfg.isDelegable();
    }

    private boolean roleIsServiceAdmin(AuthToken authToken) {
        if (authToken.getRoles() == null || serviceAdminRoles == null) {
            return false;
        }

        for (String role : authToken.getRoles().split(",")) {
            if (serviceAdminRoles.contains(role)) {
                return true;
            }
        }

        return false;
    }

    private AuthToken validateTenant(AuthenticateResponse resp, String tenantID) {
        AuthToken authToken = null;
        if (resp != null) {
            authToken = new OpenStackToken(resp);
        }

        if (authToken != null && !roleIsServiceAdmin(authToken) && !authToken.getTenantId().equals(tenantID)) {
            // tenant ID from token did not match URI.

            if (resp.getUser() != null && resp.getUser().getRoles() != null) {
                for (Role role : resp.getUser().getRoles().getRole()) {
                    if (tenantID.equals(role.getTenantId())) {
                        //we have the real tenantID
                        return authToken;
                    }
                }
            }

            DELEGATION_MESSAGE.set("Unable to validate token for tenant. Invalid token: " + authToken.getTokenId() + ".");
            LOG.error("Unable to validate token for tenant. Invalid token: " + authToken.getTokenId() + ".");
            return null;
        } else {
            return authToken;
        }
    }

    @Override
    public AuthToken validateToken(ExtractorResult<String> account, String token, String requestGuid) throws AuthServiceException {
        AuthToken authToken = null;

        if (account != null) {
            AuthenticateResponse authResponse = authenticationService.validateToken(account.getResult(), token, requestGuid);
            DELEGATION_MESSAGE.set(AuthenticationServiceClient.getDelegationMessage()); // Must be set before validateTenant call in case that call overwrites this value
            authToken = validateTenant(authResponse, account.getResult());
        } else {
            AuthenticateResponse authResp = authenticationService.validateToken(null, token, requestGuid);
            DELEGATION_MESSAGE.set(AuthenticationServiceClient.getDelegationMessage());
            if (authResp != null) {
                authToken = new OpenStackToken(authResp);
            }
        }
        AuthenticationServiceClient.removeDelegationMessage();

        /**
         * If any role in that token is in the BypassTenantRoles list, bypass the tenant check
         */
        if (authToken != null) { //authToken could still be null at this point :(
            boolean ignoreTenantRequirement = false;
            for (String role : authToken.getRoles().split(",")) {
                if (ignoreTenantRoles.contains(role)) {
                    ignoreTenantRequirement = true;
                }
            }

            if (!ignoreTenantRequirement) {
                if (authToken.getTenantId() == null || authToken.getTenantName() == null) {
                    //Moved this check from within the OpenStackToken into here
                    DELEGATION_MESSAGE.set("Invalid Response from Auth for token: " + authToken.getTokenId() + ". Token object must have a tenant");
                    throw new IllegalArgumentException("Invalid Response from Auth for token: " + authToken.getTokenId() + ". Token object must have a tenant");
                }
            }
        }
        return authToken;
    }

    @Override
    public AuthGroups getGroups(String group, String requestGuid) throws AuthServiceException {
        return authenticationService.getGroups(group, requestGuid);
    }

    @Override
    public FilterDirector processResponse(ReadableHttpServletResponse response) {
        LOG.debug("Handling service response in OpenStack auth filter.");
        final int responseStatus = response.getStatus();
        LOG.debug("Incoming response code is " + responseStatus);
        final FilterDirector myDirector = new FilterDirectorImpl();
        myDirector.setResponseStatusCode(responseStatus);
        myDirector.setFilterAction(FilterAction.PASS);

        /// The WWW Authenticate header can be used to communicate to the client
        // (since we are a proxy) how to correctly authenticate itself
        final String wwwAuthenticateHeader = response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString());

        switch (response.getStatus()) {
            // NOTE: We should only mutate the WWW-Authenticate header on a
            // 401 (unauthorized) or 403 (forbidden) response from the origin service
            case HttpServletResponse.SC_UNAUTHORIZED:
            case HttpServletResponse.SC_FORBIDDEN:
                // If in the case that the origin service supports delegated authentication
                // we should then communicate to the client how to authenticate with us
                if (delegatingMode) {
                    myDirector.responseHeaderManager().appendHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString(), wwwAuthHeaderContents);
                } else {
                    // In the case where authentication has failed and we are not in delegating mode,
                    //  this means that our own authentication with the origin service has failed
                    // and must then be communicated as a 500 (internal server error) to the client
                    myDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                break;
            case HttpServletResponse.SC_NOT_IMPLEMENTED:
                if (!StringUtils.isBlank(wwwAuthenticateHeader) && wwwAuthenticateHeader.contains(DELEGATED)) {
                    myDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    LOG.error("Repose authentication component is configured as delegetable but origin service does not support delegated mode.");
                } else {
                    myDirector.setResponseStatusCode(HttpServletResponse.SC_NOT_IMPLEMENTED);
                }
                break;

            default:
                break;
        }

        LOG.debug("Outgoing response code is " + myDirector.getResponseStatusCode());
        return myDirector;
    }

    @Override
    protected String getEndpointsBase64(String token, EndpointsConfiguration endpointsConfiguration, String requestGuid) throws AuthServiceException {
        return authenticationService.getBase64EndpointsStringForHeaders(token, endpointsConfiguration.getFormat(), requestGuid);
    }

    @Override
    public void setFilterDirectorValues(
            String authToken,
            AuthToken cachableToken,
            Boolean delegatable,
            double delegableQuality,
            String delegationMessage,
            FilterDirector filterDirector,
            String extractedResult,
            List<AuthGroup> groups,
            String endpointsInBase64,
            String contactId,
            boolean tenanted, boolean sendAllTenantIds, boolean sendTenantIdQuality) {

        new OpenStackAuthenticationHeaderManager(authToken, cachableToken, delegatable, delegableQuality, delegationMessage,
                filterDirector, extractedResult, groups, wwwAuthHeaderContents, endpointsInBase64, contactId, tenanted, sendAllTenantIds,
                sendTenantIdQuality)
                .setFilterDirectorValues();
    }
}
