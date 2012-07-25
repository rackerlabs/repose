package com.rackspace.auth.openstack.ids;

import java.util.List;
import org.openstack.docs.identity.api.v2.Endpoint;

/**
 *
 * @author zinic
 */
public interface OpenStackAuthenticationService {

    CachableUserInfo validateToken(String tenant, String userToken);
    
    List<Endpoint> getEndpointsForToken(String userToken);

    CachableGroupInfo getGroups(String userId);
}
