package org.openrepose.common.auth.openstack;

import org.openrepose.common.auth.AuthGroups;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Endpoint;

import java.util.List;

public interface AuthenticationService {

    AuthenticateResponse validateToken(String tenant, String userToken) throws AuthenticationServiceException;
    
    List<Endpoint> getEndpointsForToken(String userToken) throws AuthenticationServiceException;

    AuthGroups getGroups(String userId) throws AuthenticationServiceException;

    String getBase64EndpointsStringForHeaders(String userToken, String format) throws AuthenticationServiceException;
}
