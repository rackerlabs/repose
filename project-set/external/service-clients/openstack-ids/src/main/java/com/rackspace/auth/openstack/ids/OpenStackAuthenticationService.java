package com.rackspace.auth.openstack.ids;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import java.util.List;
import org.openstack.docs.identity.api.v2.Endpoint;

/**
 *
 * @author zinic
 */
public interface OpenStackAuthenticationService {

    CachableTokenInfo validateToken(String tenant, String userToken);
    
    List<Endpoint> getEndpointsForToken(String userToken);

    Groups getGroups(String userId);
}
