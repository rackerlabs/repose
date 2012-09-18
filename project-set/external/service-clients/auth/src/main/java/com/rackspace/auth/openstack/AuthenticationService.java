package com.rackspace.auth.openstack;

import com.rackspace.auth.AuthGroups;
import com.rackspace.auth.AuthToken;
import com.rackspace.auth.FullAuthInfo;
import org.openstack.docs.identity.api.v2.Endpoint;

import java.util.List;

/**
 *
 * @author zinic
 */
public interface AuthenticationService {

    FullAuthInfo validateToken(String tenant, String userToken);
    
    List<Endpoint> getEndpointsForToken(String userToken);

    AuthGroups getGroups(String userId);
}
