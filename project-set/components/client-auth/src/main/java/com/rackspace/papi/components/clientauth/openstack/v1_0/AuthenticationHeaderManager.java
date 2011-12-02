package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.openstack.ids.CachableTokenInfo;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.components.clientauth.rackspace.IdentityStatus;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fran
 * 
 */
public class AuthenticationHeaderManager {

    private final String authToken;
    private final CachableTokenInfo cachableTokenInfo;
    private final Boolean isDelegatable;
    private final FilterDirector filterDirector;
    private final String tenantId;
    private final Boolean validToken;
    private final Groups groups;

    public AuthenticationHeaderManager(String authToken, CachableTokenInfo cachableTokenInfo, Boolean isDelegatable, FilterDirector filterDirector, String tenantId, Groups groups) {
        this.authToken =authToken;
        this.cachableTokenInfo = cachableTokenInfo;
        this.isDelegatable = isDelegatable;
        this.filterDirector = filterDirector;
        this.tenantId = tenantId;
        this.validToken = cachableTokenInfo != null && cachableTokenInfo.getTokenId() != null;       
        this.groups = groups;
    }

    public void setFilterDirectorValues() {

        if (validToken) {
            filterDirector.setFilterAction(FilterAction.PASS);
            setExtendedAuthorization();
            setUser();
            setRoles();
            setGroups();
            setTenant();

            if (isDelegatable) {
                setIdentityStatus();
            }
        } else if (isDelegatable) {
            if (nullCredentials()) {
                filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);
                setExtendedAuthorization();
                setIdentityStatus();
            }
        }
    }

    private boolean nullCredentials() {
        return StringUtilities.isBlank(authToken) || StringUtilities.isBlank(tenantId);
    }

    /**
     * EXTENDED AUTHORIZATION
     */
    private void setExtendedAuthorization() {
        filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION.getHeaderKey(), "Proxy " + tenantId);
    }

    /**
     * IDENTITY STATUS
     */
    private void setIdentityStatus() {
        IdentityStatus identityStatus = IdentityStatus.Confirmed;

        if (!validToken) {
            identityStatus = IdentityStatus.Indeterminate;
        }

        filterDirector.requestHeaderManager().putHeader(OpenStackServiceHeader.IDENTITY_STATUS.getHeaderKey(), identityStatus.name());
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

    /**
     * GROUPS
     * The PowerApiHeader is used for Rate Limiting
     */
    private void setGroups() {
        if (groups != null) {
            List<String> groupIds = new ArrayList<String>();
            for(Group group : groups.getGroup()) {
                groupIds.add(group.getId());
            }

            filterDirector.requestHeaderManager().putHeader(PowerApiHeader.GROUPS.getHeaderKey(), groupIds.toArray(new String[0]));
        }
    }
}