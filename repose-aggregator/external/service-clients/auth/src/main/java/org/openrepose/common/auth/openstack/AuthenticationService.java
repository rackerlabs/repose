package org.openrepose.common.auth.openstack;

import org.openrepose.common.auth.AuthGroups;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Endpoint;

import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author zinic
 */
public interface AuthenticationService {

    AuthenticateResponse validateToken(String tenant, String userToken) throws TimeoutException;
    
    List<Endpoint> getEndpointsForToken(String userToken) throws TimeoutException;

    AuthGroups getGroups(String userId) throws TimeoutException;

    String getBase64EndpointsStringForHeaders(String userToken, String format) throws TimeoutException;
}
