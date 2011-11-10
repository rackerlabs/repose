package com.rackspace.papi.components.clientauth.rackspace.v2_0;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.components.clientauth.rackspace.IdentityStatus;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Role;

/**
 * @author fran
 */
public class AuthenticationHeaderManager {
    private final AuthenticateResponse authenticateResponse;
    private final Boolean isDelegatable;
    private final FilterDirector filterDirector;
    private final String tenantId;
    private final Boolean validToken;

    public AuthenticationHeaderManager(AuthenticateResponse authenticateResponse, Boolean isDelegatable, FilterDirector filterDirector, String tenantId) {
        this.authenticateResponse = authenticateResponse;
        this.isDelegatable = isDelegatable;
        this.filterDirector = filterDirector;
        this.tenantId = tenantId;

        if (authenticateResponse != null && authenticateResponse.getToken().getId() != null && authenticateResponse.getToken().getId() != null) {
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
        filterDirector.requestHeaderManager().putHeader(PowerApiHeader.USER.headerKey(), authenticateResponse.getUser().getName());
    }

    /**
     * ROLES
     */
    private void setRoles() {
        String tmp = new String();

        for (Role role : authenticateResponse.getUser().getRoles().getRole()) {
            tmp += (role + ",");
        }

        String roles = "";
        if (tmp.endsWith(",")) {
            roles = tmp.substring(0, tmp.length() - 1);
        }

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
