package com.rackspace.auth.openstack.ids;

/**
 *
 * @author zinic
 */
public interface OpenStackAuthenticationService {

    CachableTokenInfo validateToken(String tenant, String userToken);
}
