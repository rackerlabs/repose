package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.openstack.ids.CachableTokenInfo;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.components.clientauth.rackspace.IdentityStatus;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;

/**
 * @author fran
 * 
 */
public class AuthenticationHeaderManager {

    private final CachableTokenInfo cachableTokenInfo;
    private final Boolean isDelegatable;
    private final FilterDirector filterDirector;
    private final String tenantId;
    private final Boolean validToken;

    public AuthenticationHeaderManager(CachableTokenInfo cachableTokenInfo, Boolean isDelegatable, FilterDirector filterDirector, String tenantId) {
        this.cachableTokenInfo = cachableTokenInfo;
        this.isDelegatable = isDelegatable;
        this.filterDirector = filterDirector;
        this.tenantId = tenantId;
        this.validToken = cachableTokenInfo != null && cachableTokenInfo.getTokenId() != null;       
    }

    public void setFilterDirectorValues() {
        setExtendedAuthorization();

        if (validToken) {
            filterDirector.setFilterAction(FilterAction.PASS);
        } else if (isDelegatable) {
            filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);
        }
        
        if (validToken) {
            setUser();
            setRoles();
            setTenant();
        }

        setIdentityStatus();
    }

    /**
     * EXTENDED AUTHORIZATION
     */
    private void setExtendedAuthorization() {
        filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION.getHeaderKey(), "proxy " + tenantId);
    }

    /**
     * IDENTITY STATUS
     */
    private void setIdentityStatus() {
        if (isDelegatable) {
            IdentityStatus identityStatus = IdentityStatus.Confirmed;

            if (!validToken) {
                identityStatus = IdentityStatus.Indeterminate;
            }

            filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.IDENTITY_STATUS.getHeaderKey(), identityStatus.name());
        }
    }

    /**
     * TENANT
     */
    private void setTenant() {
        filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.TENANT_NAME.getHeaderKey(), tenantId);
        filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.TENANT_ID.getHeaderKey(), tenantId);
    }

    /**
     * USER
     * The PowerApiHeader is used for Rate Limiting
     * The OpenStackServiceHeader is used for an OpenStack service
     */
    private void setUser() {
        filterDirector.requestHeaderManager().putHeader(PowerApiHeader.USER.getHeaderKey(), cachableTokenInfo.getUsername());

        filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.USER_NAME.getHeaderKey(), cachableTokenInfo.getUsername());
        filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.USER_ID.getHeaderKey(), cachableTokenInfo.getUserId());
    }

    /**
     * ROLES
     * The OpenStackServiceHeader is used for an OpenStack service
     */
    private void setRoles() {
        String roles = cachableTokenInfo.getRoles();

        if (StringUtilities.isNotBlank(roles)) {
            filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.ROLES.getHeaderKey(), roles);
        }
    }
}
