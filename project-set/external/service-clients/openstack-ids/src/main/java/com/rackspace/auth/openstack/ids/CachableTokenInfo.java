package com.rackspace.auth.openstack.ids;

import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Role;

import java.io.Serializable;

/**
 * @author fran
 */
public class CachableTokenInfo implements Serializable {

    private final String tokenId;
    private final String username;
    private final String roles;

    public CachableTokenInfo(AuthenticateResponse response) {
        this.tokenId = response.getToken().getId();
        this.username = response.getUser().getName();
        this.roles = formatRoles(response);
    }

    // TODO: Consider using string builder
    private String formatRoles(AuthenticateResponse response) {
        String tmp = new String();

        for (Role role : response.getUser().getRoles().getRole()) {
            tmp += (role + ",");
        }

        String formattedRoles = "";
        if (tmp.endsWith(",")) {
            formattedRoles = tmp.substring(0, tmp.length() - 1);
        }

        return formattedRoles;
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getUsername() {
        return username;
    }

    public String getRoles() {
        return roles;
    }
}
