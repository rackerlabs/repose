package com.rackspace.auth.openstack.ids;

import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Role;

import java.io.Serializable;

/**
 * @author fran
 */
public class CachableTokenInfo implements Serializable {

    private final String tokenId;
    private final String userId;
    private final String username;
    private final String roles;

    public CachableTokenInfo(AuthenticateResponse response) {
        this.tokenId = response.getToken().getId();
        this.userId = response.getUser().getId();
        this.username = response.getUser().getName();
        this.roles = formatRoles(response);
    }

    private String formatRoles(AuthenticateResponse response) {
        String formattedRoles = null;

        if (response.getUser() != null && response.getUser().getRoles() != null) {
            StringBuilder result = new StringBuilder();
            for(Role role : response.getUser().getRoles().getRole()) {
                result.append(role.getName());
                result.append(",");
            }

            formattedRoles = result.substring(0, result.length() - 1);
        }

        return formattedRoles;
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
}
