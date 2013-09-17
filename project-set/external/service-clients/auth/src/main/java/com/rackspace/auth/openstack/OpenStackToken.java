package com.rackspace.auth.openstack;

import com.rackspace.auth.AuthToken;
import com.rackspace.papi.commons.util.StringUtilities;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * Implementation of AuthToken {@link com.rackspace.auth.AuthToken} to parse the AuthenticationResponse from an Openstack Identity Service
 */
public class OpenStackToken extends AuthToken implements Serializable {

    private final String tenantId;
    private final String tenantName;
    private final long expires;
    private final String roles;
    private final String tokenId;
    private final String userId;
    private final String username;
    private final String impersonatorTenantId;
    private final String impersonatorUsername;
    private final String defaultRegion;
    private static final Logger LOG = LoggerFactory.getLogger(OpenStackToken.class);
    private static final QName REGION_QNAME = new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "defaultRegion", "prefix");

    public OpenStackToken(AuthenticateResponse response) {

        checkTokenInfo(response);
        this.tenantId = response.getToken().getTenant().getId();
        this.tenantName = response.getToken().getTenant().getName();
        this.expires = response.getToken().getExpires().toGregorianCalendar().getTimeInMillis();
        this.roles = formatRoles(response);
        this.tokenId = response.getToken().getId();
        this.userId = response.getUser().getId();
        this.username = response.getUser().getName();
        UserForAuthenticateResponse impersonator = getImpersonator(response);

        this.defaultRegion = getDefaultRegion(response);
        if (impersonator != null) {
            this.impersonatorTenantId = impersonator.getId();
            this.impersonatorUsername = impersonator.getName();
        } else {
            this.impersonatorTenantId = "";
            this.impersonatorUsername = "";
        }
    }

    /**
     * Assumption here is that not having a tenant would throw an exception - B-52709
     * @param response
     */
    private void checkTokenInfo(AuthenticateResponse response) {
        if (response == null || response.getToken() == null || response.getToken().getExpires() == null) {
            throw new IllegalArgumentException("Invalid token");
        }

        if (response.getToken().getTenant() == null) {
            throw new IllegalArgumentException("Invalid Response from Auth. Token object must have a tenant");
        }

        if (response.getUser() == null) {
            throw new IllegalArgumentException("Invalid Response from Auth: Response must have a user object");
        }

        if (response.getUser().getRoles() == null) {
            throw new IllegalArgumentException("Invalid Response from Auth: User must have a list of roles");
        }

    }

    private String getDefaultRegion(AuthenticateResponse response) {
        return StringUtilities.getNonBlankValue(response.getUser().getOtherAttributes().get(REGION_QNAME), "");
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public String getTenantName() {
        return this.tenantName;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getTokenId() {
        return tokenId;
    }

    @Override
    public long getExpires() {
        return expires;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getRoles() {
        return roles;
    }

    @Override
    public String getImpersonatorTenantId() {
        return impersonatorTenantId;
    }

    @Override
    public String getImpersonatorUsername() {
        return impersonatorUsername;
    }

    private UserForAuthenticateResponse getImpersonator(AuthenticateResponse response) {
        if (response.getAny() == null) {
            return null;
        }

        for (Object any : response.getAny()) {
            if (any instanceof JAXBElement) {
                JAXBElement element = (JAXBElement) any;
                if (element.getValue() instanceof UserForAuthenticateResponse) {
                    return (UserForAuthenticateResponse) element.getValue();
                }
            }
        }

        return null;
    }

    private String formatRoles(AuthenticateResponse response) {
        String formattedRoles = null;

        if (response.getUser() != null && response.getUser().getRoles() != null) {
            StringBuilder result = new StringBuilder();
            for (Role role : response.getUser().getRoles().getRole()) {
                result.append(role.getName());
                result.append(",");
            }

            if (!StringUtilities.isBlank(result.toString())) {
                formattedRoles = result.substring(0, result.length() - 1);
            } else {
                LOG.warn("User with userId " + response.getUser().getId() + " is not associated with any role");
            }
        }

        return formattedRoles;
    }

    @Override
    public String getDefaultRegion() {
        return defaultRegion;
    }
}
