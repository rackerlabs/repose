package org.openrepose.common.auth.openstack;

import org.openrepose.common.auth.AuthGroups;
import org.openrepose.services.serviceclient.akka.AkkaServiceClientException;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Endpoint;

import java.util.List;

/**
 *
 * @author zinic
 */
public interface AuthenticationService {

    AuthenticateResponse validateToken(String tenant, String userToken) throws AkkaServiceClientException;
    
    List<Endpoint> getEndpointsForToken(String userToken) throws AkkaServiceClientException;

    AuthGroups getGroups(String userId) throws AkkaServiceClientException;

    String getBase64EndpointsStringForHeaders(String userToken, String format) throws AkkaServiceClientException;
}
