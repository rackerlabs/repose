package com.rackspace.auth.openstack.ids;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;

/**
 *
 * @author zinic
 */
public interface OpenStackAuthenticationService {

    CachableTokenInfo validateToken(String tenant, String userToken);

    Groups getGroups(String userId);
}
