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

import com.rackspace.httpdelegation.JavaDelegationManagerProxy;
import org.openrepose.common.auth.AuthGroup;
import org.openrepose.common.auth.AuthToken;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.*;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Responsible for adding Authentication headers from validating token response
 */
@Deprecated
public class OpenStackAuthenticationHeaderManager {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(OpenStackAuthenticationHeaderManager.class);
    // Proxy is specified in the OpenStack auth blue print:
    // http://wiki.openstack.org/openstack-authn
    private static final String X_AUTH_PROXY = "Proxy";
    private final String authToken;
    private final AuthToken cachableToken;
    private final Boolean isDelagable;
    private final double delegableQuality;
    private final String delegationMessage;
    private final FilterDirector filterDirector;
    private final String tenantId;
    private final Boolean validToken;
    private final List<AuthGroup> groups;
    private final String wwwAuthHeaderContents;
    private final String endpointsBase64;
    private final String contactId;
    private final boolean sendAllTenantIds;
    private final boolean sendTenantIdQuality;

    //add base 64 string in here
    public OpenStackAuthenticationHeaderManager(String authToken, AuthToken token, Boolean isDelegatable,
                                                double delegableQuality, String delegationMessage,
                                                FilterDirector filterDirector, String tenantId, List<AuthGroup> groups,
                                                String wwwAuthHeaderContents, String endpointsBase64, String contactId,
                                                boolean sendAllTenantIds, boolean sendTenantIdQuality) {
        this.authToken = authToken;
        this.cachableToken = token;
        this.isDelagable = isDelegatable;
        this.delegableQuality = delegableQuality;
        this.delegationMessage = delegationMessage;
        this.filterDirector = filterDirector;
        this.tenantId = tenantId;
        this.validToken = token != null && token.getTokenId() != null;
        this.groups = groups;
        this.wwwAuthHeaderContents = wwwAuthHeaderContents;
        this.endpointsBase64 = endpointsBase64;
        this.contactId = contactId;
        this.sendAllTenantIds = sendAllTenantIds;
        this.sendTenantIdQuality = sendTenantIdQuality;
    }

    //set header with base64 string here
    public void setFilterDirectorValues() {
        int responseStatusCode = filterDirector.getResponseStatusCode();
        if (validToken
                && responseStatusCode != HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                && responseStatusCode != HttpServletResponse.SC_SERVICE_UNAVAILABLE) {
            filterDirector.setFilterAction(FilterAction.PASS);
            setExtendedAuthorization();
            setUser();
            setRoles();
            setGroups();
            setTenant();
            setImpersonator();
            setEndpoints();
            setExpirationDate();
            setDefaultRegion();
            setContactId();

            if (isDelagable) {
                setIdentityStatus();
            }
        } else if (isDelagable
                && nullCredentials()
                && responseStatusCode != HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                && responseStatusCode != HttpServletResponse.SC_SERVICE_UNAVAILABLE) {
            filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);
            setExtendedAuthorization();
            setIdentityStatus();
            setDelegationHeader();
        } else if (isDelagable) {
            filterDirector.setFilterAction(FilterAction.PASS);
            setExtendedAuthorization();
            setIdentityStatus();
            setDelegationHeader();
            filterDirector.setResponseStatusCode(200); // Note: The response status code must be set to a non-500 so that the request will be routed appropriately.
        } else if (responseStatusCode == HttpServletResponse.SC_UNAUTHORIZED) {
            filterDirector.responseHeaderManager().putHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString(), wwwAuthHeaderContents);
        }
    }

    private boolean nullCredentials() {
        final boolean nullCreds = StringUtilities.isBlank(authToken) && StringUtilities.isBlank(tenantId);

        LOG.debug("Credentials null = " + nullCreds);
        return nullCreds;
    }

    /**
     * EXTENDED AUTHORIZATION
     */
    private void setExtendedAuthorization() {
        filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION.toString(), StringUtilities.isBlank(tenantId) ? X_AUTH_PROXY : X_AUTH_PROXY + " " + tenantId);
    }

    /**
     * IDENTITY STATUS
     */
    private void setIdentityStatus() {
        IdentityStatus identityStatus = IdentityStatus.Confirmed;

        if (!validToken) {
            identityStatus = IdentityStatus.Indeterminate;
        }

        filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.IDENTITY_STATUS.toString(), identityStatus.name());
    }

    private void setDelegationHeader() {
        for (Map.Entry<String, List<String>> headerEntry : JavaDelegationManagerProxy.buildDelegationHeaders(
                filterDirector.getResponseStatusCode(), "client-auth-n", delegationMessage, delegableQuality).entrySet()) {
            List<String> headerValues = headerEntry.getValue();
            filterDirector.requestHeaderManager().appendHeader(headerEntry.getKey(), headerValues.toArray(new String[headerValues.size()]));
        }
    }

    private void setImpersonator() {
        filterDirector.requestHeaderManager().removeHeader(OpenStackServiceHeader.IMPERSONATOR_NAME.toString());
        filterDirector.requestHeaderManager().removeHeader(OpenStackServiceHeader.IMPERSONATOR_ID.toString());

        if (StringUtilities.isNotBlank(cachableToken.getImpersonatorTenantId())) {
            filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.IMPERSONATOR_NAME.toString(), cachableToken.getImpersonatorUsername());
            filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.IMPERSONATOR_ID.toString(), cachableToken.getImpersonatorTenantId());
        }

        if (!cachableToken.getImpersonatorRoles().isEmpty()) {
            StringBuilder roles = new StringBuilder();
            for (String role : cachableToken.getImpersonatorRoles()) {
                roles.append(role);
                roles.append(',');
            }
            filterDirector.requestHeaderManager().appendHeader(OpenStackServiceHeader.IMPERSONATOR_ROLES.toString(), roles.substring(0, roles.length() - 1));
        }
    }

    /**
     * TENANT
     */
    private void setTenant() {
        filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.TENANT_NAME.toString(), cachableToken.getTenantName());

        // IMPORTANT: http://goo.gl/aDKbST
        if (sendAllTenantIds && sendTenantIdQuality) {
            if (cachableToken.getTenantId() != null) {
                filterDirector.requestHeaderManager().appendHeader(OpenStackServiceHeader.TENANT_ID.toString(), cachableToken.getTenantId(), 1.0);
            }
            for (String id : cachableToken.getTenantIds()) {
                if (!StringUtilities.nullSafeEquals(id, cachableToken.getTenantId())) {
                    filterDirector.requestHeaderManager().appendHeader(OpenStackServiceHeader.TENANT_ID.toString(), id, 0.5);
                }
            }
        } else if (sendAllTenantIds && !sendTenantIdQuality) {
            if (cachableToken.getTenantId() != null) {
                filterDirector.requestHeaderManager().appendHeader(OpenStackServiceHeader.TENANT_ID.toString(), cachableToken.getTenantId());
            }
            for (String id : cachableToken.getTenantIds()) {
                if (!StringUtilities.nullSafeEquals(id, cachableToken.getTenantId())) {
                    filterDirector.requestHeaderManager().appendHeader(OpenStackServiceHeader.TENANT_ID.toString(), id);
                }
            }
        } else if (!sendAllTenantIds && sendTenantIdQuality) {
            if (cachableToken.getMatchingTenantId() != null) {
                filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.TENANT_ID.toString(), cachableToken.getMatchingTenantId(), 1.0);
            } else if (cachableToken.getTenantId() != null) {
                filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.TENANT_ID.toString(), cachableToken.getTenantId(), 1.0);
            } else if (!cachableToken.getTenantIds().isEmpty()) {
                filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.TENANT_ID.toString(), cachableToken.getTenantIds().iterator().next(), 1.0);
            }
        } else {
            if (cachableToken.getMatchingTenantId() != null) {
                filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.TENANT_ID.toString(), cachableToken.getMatchingTenantId());
            } else if (cachableToken.getTenantId() != null) {
                filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.TENANT_ID.toString(), cachableToken.getTenantId());
            } else if (!cachableToken.getTenantIds().isEmpty()) {
                filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.TENANT_ID.toString(), cachableToken.getTenantIds().iterator().next());
            }
        }
    }

    /**
     * USER
     * The PowerApiHeader is used for Rate Limiting
     * The OpenStackServiceHeader is used for an OpenStack service
     */
    private void setUser() {
        filterDirector.requestHeaderManager().appendHeader(PowerApiHeader.USER.toString(), cachableToken.getUsername());

        filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.USER_NAME.toString(), cachableToken.getUsername());
        filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.USER_ID.toString(), cachableToken.getUserId());
    }

    /**
     * ROLES
     * The OpenStackServiceHeader is used for an OpenStack service
     */
    private void setRoles() {
        String roles = cachableToken.getRoles();

        if (StringUtilities.isNotBlank(roles)) {
            filterDirector.requestHeaderManager().appendHeader(OpenStackServiceHeader.ROLES.toString(), roles);
        }
    }

    /**
     * GROUPS
     * The PowerApiHeader is used for Rate Limiting
     */
    private void setGroups() {
        for (AuthGroup group : groups) {
            filterDirector.requestHeaderManager().appendHeader(PowerApiHeader.GROUPS.toString(), group.getId());
        }
    }

    /**
     * ENDPOINTS
     * The base 64 encoded list of endpoints in an x-catalog header.
     */
    private void setEndpoints() {
        if (!StringUtilities.isBlank(endpointsBase64)) {
            filterDirector.requestHeaderManager().putHeader(PowerApiHeader.X_CATALOG.toString(), endpointsBase64);
        }
    }

    /**
     * ExpirationDate
     * token expiration date in x-token-expires header that follows http spec rfc1123 GMT time format
     */

    private void setExpirationDate() {
        if (cachableToken.getExpires() > 0) {
            HttpDate date = new HttpDate(new Date(cachableToken.getExpires()));
            filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.X_EXPIRATION.toString(), date.toRFC1123());
        }
    }


    /**
     * Default Region
     * Default region of user
     */
    private void setDefaultRegion() {
        String region = cachableToken.getDefaultRegion();
        if (!StringUtilities.isBlank(region)) {
            filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.DEFAULT_REGION.toString(), region);
        }
    }

    private void setContactId() {
        if (contactId != null) {
            filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.CONTACT_ID.toString(), contactId);
        }
    }
}