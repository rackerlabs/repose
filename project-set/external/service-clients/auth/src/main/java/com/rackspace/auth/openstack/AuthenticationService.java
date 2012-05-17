package com.rackspace.auth.openstack;

import com.rackspace.auth.AuthGroup;
import com.rackspace.auth.AuthToken;

import java.util.List;
import org.openstack.docs.identity.api.v2.Endpoint;

/**
 *
 * @author zinic
 */
public interface AuthenticationService {

    AuthToken validateToken(String tenant, String userToken);
    
    List<Endpoint> getEndpointsForToken(String userToken);

    List<AuthGroup> getGroups(String userId);
}
