package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.openstack.ids.CachableTokenInfo;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.components.clientauth.rackspace.IdentityStatus;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;

/**
 * @author fran
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

        if (cachableTokenInfo != null && cachableTokenInfo.getTokenId() != null) {
            this.validToken = true;
        } else {
            this.validToken = false;
        }
    }

    public void setFilterDirectorValues() {
        setExtendedAuthorization();

        if (validToken || isDelegatable) {
            filterDirector.setFilterAction(FilterAction.PASS);
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
        filterDirector.requestHeaderManager().putHeader(CommonHttpHeader.EXTENDED_AUTHORIZATION.headerKey(), "proxy " + tenantId);
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

            filterDirector.requestHeaderManager().putHeader(CommonHttpHeader.IDENTITY_STATUS.headerKey(), identityStatus.name());
        }
    }

    /**
     * USER
     */
    private void setUser() {
        filterDirector.requestHeaderManager().putHeader(PowerApiHeader.USER.headerKey(), cachableTokenInfo.getUsername());
    }

    /**
     * ROLES
     */
    private void setRoles() {
        String roles = cachableTokenInfo.getRoles();

        if (StringUtilities.isNotBlank(roles)) {
            filterDirector.requestHeaderManager().putHeader(PowerApiHeader.GROUPS.headerKey(), roles);
            filterDirector.requestHeaderManager().putHeader(PowerApiHeader.ROLES.headerKey(), roles);
        }
    }

    /**
     * TENANT
     */
    private void setTenant() {
        filterDirector.requestHeaderManager().putHeader(PowerApiHeader.USER.headerKey(), tenantId);
        filterDirector.requestHeaderManager().putHeader(PowerApiHeader.TENANT_NAME.headerKey(), tenantId);
        filterDirector.requestHeaderManager().putHeader(PowerApiHeader.TENANT.headerKey(), tenantId);
        filterDirector.requestHeaderManager().putHeader(PowerApiHeader.TENANT_ID.headerKey(), tenantId);
    }
}
