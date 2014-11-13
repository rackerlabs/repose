package org.openrepose.filters.clientauth.openstack.v1_0;

import com.rackspace.httpdelegation.JavaDelegationManagerProxy;
import com.rackspace.httpdelegation.impl.HttpDelegationManagerImpl;
import org.openrepose.common.auth.AuthGroup;
import org.openrepose.common.auth.AuthToken;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.*;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.slf4j.Logger;
import scala.collection.JavaConverters;
import scala.collection.immutable.Set;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Responsible for adding Authentication headers from validating token response
 */
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
    private final Boolean isTenanted;
    private final FilterDirector filterDirector;
    private final String tenantId;
    private final Boolean validToken;
    private final List<AuthGroup> groups;
    // Hard code QUALITY for now as the auth component will have
    // the highest QUALITY in terms of using the user it supplies for rate limiting
    private static final String QUALITY = ";q=1.0";
    private final String wwwAuthHeaderContents;
    private final String endpointsBase64;
    private final boolean sendAllTenantIds;

    //add base 64 string in here
    public OpenStackAuthenticationHeaderManager(String authToken, AuthToken token, Boolean isDelegatable,
                                                double delegableQuality, String delegationMessage,
                                                FilterDirector filterDirector, String tenantId, List<AuthGroup> groups,
                                                String wwwAuthHeaderContents, String endpointsBase64, boolean tenanted,
                                                boolean sendAllTenantIds) {
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
        this.isTenanted = tenanted;
        this.sendAllTenantIds = sendAllTenantIds;
    }

    //set header with base64 string here
    public void setFilterDirectorValues() {
        if (validToken && filterDirector.getResponseStatus() != HttpStatusCode.INTERNAL_SERVER_ERROR) {
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

            if (isDelagable) {
                setIdentityStatus();
            }
        } else if (isDelagable && nullCredentials() && filterDirector.getResponseStatus() != HttpStatusCode.INTERNAL_SERVER_ERROR) {
            filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);
            setExtendedAuthorization();
            setIdentityStatus();
            setDelegationHeader();
        } else if (isDelagable) {
            filterDirector.setFilterAction(FilterAction.PASS);
            setExtendedAuthorization();
            setIdentityStatus();
            setDelegationHeader();
        } else if (filterDirector.getResponseStatusCode() == HttpStatusCode.UNAUTHORIZED.intValue()) {
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
    }

    /**
    * TENANT
    */
    private void setTenant() {
        filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.TENANT_NAME.toString(), cachableToken.getTenantName());
        if(!this.isDelagable && !this.isTenanted) {
            filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.TENANT_ID.toString(), cachableToken.getTenantId());
        } else {
            filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.TENANT_ID.toString(), this.tenantId);
        }

        if(sendAllTenantIds) {
            for(String id : cachableToken.getTenantIds()) {
                filterDirector.requestHeaderManager().appendHeader(OpenStackServiceHeader.TENANT_ID.toString(), id);
            }
        }
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

    /**
     * ExpirationDate
     * token expiration date in x-token-expires header that follows http spec rfc1123 GMT time format
     */

    private void setExpirationDate() {
        if (cachableToken.getExpires()>0) {
            HttpDate date = new HttpDate(new Date(cachableToken.getExpires()));
            filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.X_EXPIRATION.toString(), date.toRFC1123());
        }
    }



    /**
     * Default Region
     * Default region of user
     */
    private void setDefaultRegion(){
       String region = cachableToken.getDefaultRegion();
       if(!StringUtilities.isBlank(region)){
          filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.DEFAULT_REGION.toString(), region);
       }
    }
}