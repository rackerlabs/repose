/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.common.auth.openstack;

import org.openrepose.common.auth.AuthToken;
import org.openrepose.commons.utils.StringUtilities;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of AuthToken {@link org.openrepose.common.auth.AuthToken} to parse the AuthenticationResponse from an Openstack Identity Service
 */
public class OpenStackToken extends AuthToken implements Serializable {

    public static final QName CONTACT_ID_QNAME = new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "contactId", "prefix");
    private static final Logger LOG = LoggerFactory.getLogger(OpenStackToken.class);
    private static final QName REGION_QNAME = new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "defaultRegion", "prefix");
    private final String tenantId;
    private final String tenantName;
    private final long expires;
    private final String roles;
    private final String tokenId;
    private final String userId;
    private final String username;
    private final String impersonatorTenantId;
    private final String impersonatorUsername;
    private final Set<String> impersonatorRoles;
    private final String defaultRegion;
    private final Set<String> tenantIds;
    private final String contactId;
    private String matchingTenantId;

    public OpenStackToken(AuthenticateResponse response) {

        checkTokenInfo(response);

        //Tenant is now optional
        if (response.getToken().getTenant() != null) {
            this.tenantId = response.getToken().getTenant().getId();
            this.tenantName = response.getToken().getTenant().getName();
        } else {
            this.tenantId = null;
            this.tenantName = null;
        }
        this.expires = response.getToken().getExpires().toGregorianCalendar().getTimeInMillis();
        this.roles = formatRoles(response);
        this.tokenId = response.getToken().getId();
        this.userId = response.getUser().getId();
        this.username = response.getUser().getName();
        UserForAuthenticateResponse impersonator = getImpersonator(response);

        this.defaultRegion = getDefaultRegion(response);
        this.impersonatorRoles = new HashSet<>();
        if (impersonator != null) {
            this.impersonatorTenantId = impersonator.getId();
            this.impersonatorUsername = impersonator.getName();
            for (Role role : impersonator.getRoles().getRole()) {
                impersonatorRoles.add(role.getName());
            }
        } else {
            this.impersonatorTenantId = "";
            this.impersonatorUsername = "";
        }

        tenantIds = new HashSet<>();
        for (Role role : response.getUser().getRoles().getRole()) {
            String tid = role.getTenantId();
            if (tid != null) {
                tenantIds.add(tid);
            }
        }
        this.contactId = response.getUser().getOtherAttributes().get(CONTACT_ID_QNAME);
    }

    /**
     * Assumption here is that not having a tenant would throw an exception - B-52709
     * This is no longer valid, Some responses will not have a tenant. - B-60838
     * There is logic elsewhere that will double check the existence of a tenant based on configuration data
     *
     * @param response
     */
    private void checkTokenInfo(AuthenticateResponse response) {
        if (response == null || response.getToken() == null || response.getToken().getExpires() == null) {
            throw new IllegalArgumentException("Invalid token");
        }

        if (response.getUser() == null) {
            throw new IllegalArgumentException("Invalid Response from Auth: Response must have a user object");
        }

        if (response.getUser().getRoles() == null) {
            throw new IllegalArgumentException("Invalid Response from Auth: User must have a list of roles");
        }
    }

    @Override
    public void setMatchingTenantId(String tenantId) {
        this.matchingTenantId = tenantId;
    }

    private String getDefaultRegion(AuthenticateResponse response) {
        return StringUtilities.getNonBlankValue(response.getUser().getOtherAttributes().get(REGION_QNAME), "");
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public String getMatchingTenantId() {
        return matchingTenantId;
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

    @Override
    public Set<String> getImpersonatorRoles() {
        return impersonatorRoles;
    }

    @Override
    public Set<String> getTenantIds() {
        return tenantIds;
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

    @Override
    public String getContactId() {
        return contactId;
    }
}
