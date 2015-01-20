package org.openrepose.common.auth.openstack;

import org.openrepose.common.auth.AuthGroups;
import org.openrepose.services.serviceclient.akka.AkkServiceClientException;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Endpoint;

import java.util.List;

/**
 *
 * @author zinic
 */
public interface AuthenticationService {

    AuthenticateResponse validateToken(String tenant, String userToken) throws AkkServiceClientException;
    
    List<Endpoint> getEndpointsForToken(String userToken) throws AkkServiceClientException;

    AuthGroups getGroups(String userId) throws AkkServiceClientException;

    String getBase64EndpointsStringForHeaders(String userToken, String format) throws AkkServiceClientException;
}
