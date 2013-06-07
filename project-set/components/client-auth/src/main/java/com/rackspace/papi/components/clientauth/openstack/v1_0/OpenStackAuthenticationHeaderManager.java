package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.AuthGroup;
import com.rackspace.auth.AuthToken;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.IdentityStatus;
import com.rackspace.papi.commons.util.http.OpenStackServiceHeader;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import org.slf4j.Logger;

import java.util.List;

/**
 * @author fran
 */
public class OpenStackAuthenticationHeaderManager {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(OpenStackAuthenticationHeaderManager.class);
    // Proxy is specified in the OpenStack auth blue print:
    // http://wiki.openstack.org/openstack-authn
    private static final String X_AUTH_PROXY = "Proxy";
    private final String authToken;
    private final AuthToken cachableToken;
    private final Boolean isDelagable;
    private final FilterDirector filterDirector;
    private final String tenantId;
    private final Boolean validToken;
    private final List<AuthGroup> groups;
    // Hard code QUALITY for now as the auth component will have
    // the highest QUALITY in terms of using the user it supplies for rate limiting
    private static final String QUALITY = ";q=1.0";
    private final String wwwAuthHeaderContents;
    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    private final String endpointsBase64;

    //add base 64 string in here
    public OpenStackAuthenticationHeaderManager(String authToken, AuthToken token, Boolean isDelegatable,
            FilterDirector filterDirector, String tenantId, List<AuthGroup> groups, String wwwAuthHeaderContents, String endpointsBase64) {
        this.authToken = authToken;
        this.cachableToken = token;
        this.isDelagable = isDelegatable;
        this.filterDirector = filterDirector;
        this.tenantId = tenantId;
        this.validToken = token != null && token.getTokenId() != null;
        this.groups = groups;
        this.wwwAuthHeaderContents = wwwAuthHeaderContents;
        this.endpointsBase64 = endpointsBase64;
    }

    //set header with base64 string here
    public void setFilterDirectorValues() {
        if (validToken) {
            filterDirector.setFilterAction(FilterAction.PASS);
            setExtendedAuthorization();
            setUser();
            setRoles();
            setGroups();
            setTenant();
            setImpersonator();
            setEndpoints();

            if (isDelagable) {
                setIdentityStatus();
            }
        } else if (isDelagable && nullCredentials()) {
            filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);
            setExtendedAuthorization();
            setIdentityStatus();
        } else if (filterDirector.getResponseStatusCode() == HttpStatusCode.UNAUTHORIZED.intValue()) {
            filterDirector.responseHeaderManager().putHeader(WWW_AUTHENTICATE_HEADER, wwwAuthHeaderContents);
        }
    }

    private boolean nullCredentials() {
        final boolean nullCreds = StringUtilities.isBlank(authToken) || StringUtilities.isBlank(tenantId);

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

    private void setImpersonator() {
        filterDirector.requestHeaderManager().removeHeader(OpenStackServiceHeader.IMPERSONATOR_NAME.toString());
        filterDirector.requestHeaderManager().removeHeader(OpenStackServiceHeader.IMPERSONATOR_ID.toString());

        if (StringUtilities.isNotBlank(cachableToken.getImpersonatorTenantId())) {
            filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.IMPERSONATOR_NAME.toString(), cachableToken.getImpersonatorUsername());
            filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.IMPERSONATOR_ID.toString(), cachableToken.getImpersonatorTenantId());
        }
    }

    /**
    * TENANT
    */
    private void setTenant() {
        filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.TENANT_NAME.toString(), cachableToken.getTenantName());
        filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.TENANT_ID.toString(), cachableToken.getTenantId());
    }

    /**
    * USER
    * The PowerApiHeader is used for Rate Limiting
    * The OpenStackServiceHeader is used for an OpenStack service
    */
    private void setUser() {
        filterDirector.requestHeaderManager().appendHeader(PowerApiHeader.USER.toString(), cachableToken.getUsername() + QUALITY);

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
            filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.ROLES.toString(), roles);
        }
    }

    /**
    * GROUPS
    * The PowerApiHeader is used for Rate Limiting
    */
    private void setGroups() {
        for (AuthGroup group : groups) {
            filterDirector.requestHeaderManager().appendHeader(PowerApiHeader.GROUPS.toString(), group.getId() + QUALITY);
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
}