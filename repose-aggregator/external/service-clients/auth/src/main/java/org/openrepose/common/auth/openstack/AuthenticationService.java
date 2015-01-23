package org.openrepose.common.auth.openstack;

import org.openrepose.common.auth.AuthGroups;
import org.openrepose.common.auth.AuthServiceException;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Endpoint;

import java.util.List;

public interface AuthenticationService {

    AuthenticateResponse validateToken(String tenant, String userToken) throws AuthServiceException;
    
    List<Endpoint> getEndpointsForToken(String userToken) throws AuthServiceException;

    AuthGroups getGroups(String userId) throws AuthServiceException;

    String getBase64EndpointsStringForHeaders(String userToken, String format) throws AuthServiceException;
}
