package com.rackspace.auth.v2_0;

import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Role;

/**
 * @author fran
 */
public class CachableTokenInfo {
    private final String tokenId;
    private final String username;
    private final String roles;

    public CachableTokenInfo(AuthenticateResponse response) {
        this.tokenId = response.getToken().getId();
        this.username = response.getUser().getName();
        this.roles = formatRoles(response);
    }

    private String formatRoles(AuthenticateResponse response) {
        String tmp = new String();

        for (Role role : response.getUser().getRoles().getRole()) {
            tmp += (role + ",");
        }

        String roles = "";
        if (tmp.endsWith(",")) {
            roles = tmp.substring(0, tmp.length() - 1);
        }

        return roles;
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
