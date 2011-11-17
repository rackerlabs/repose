package com.rackspace.auth.openstack.ids;

import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Role;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fran
 */
public class CachableTokenInfo implements Serializable {

    private final String tokenId;
    private final String userId;
    private final String username;
    private final String roles;
    private final String[] roleNames;

    public CachableTokenInfo(AuthenticateResponse response) {
        this.tokenId = response.getToken().getId();
        this.userId = response.getUser().getId();
        this.username = response.getUser().getName();
        this.roles = formatRoles(response);
        this.roleNames = formatRoleNames(response);
    }

    private String formatRoles(AuthenticateResponse response) {

        StringBuilder result = new StringBuilder();
        for(Role role : response.getUser().getRoles().getRole()) {
            result.append(role);
            result.append(",");
        }

        return result.substring(0, result.length() - 1);
    }

    private String[] formatRoleNames(AuthenticateResponse response) {

        List<String> roleNames = new ArrayList<String>();
        for(Role role : response.getUser().getRoles().getRole()) {
            roleNames.add(role.getName());
        }

        return roleNames.toArray(new String[0]);
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRoles() {
        return roles;
    }

    public String[] getRoleNames() {
        return roleNames;
    }
}
