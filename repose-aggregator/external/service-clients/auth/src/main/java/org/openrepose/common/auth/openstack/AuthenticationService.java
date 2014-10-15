package org.openrepose.common.auth.openstack;

import org.openrepose.common.auth.AuthGroups;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Endpoint;

import java.util.List;

/**
 *
 * @author zinic
 */
public interface AuthenticationService {

    AuthenticateResponse validateToken(String tenant, String userToken);
    
    List<Endpoint> getEndpointsForToken(String userToken);

    AuthGroups getGroups(String userId);

    String getBase64EndpointsStringForHeaders(String userToken, String format);
}
