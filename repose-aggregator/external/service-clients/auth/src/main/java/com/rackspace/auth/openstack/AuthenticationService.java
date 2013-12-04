package com.rackspace.auth.openstack;

import com.rackspace.auth.AuthGroups;
import com.rackspace.auth.AuthToken;
import com.yammer.metrics.core.Meter;
import org.openstack.docs.identity.api.v2.Endpoint;

import java.util.List;

/**
 *
 * @author zinic
 */
public interface AuthenticationService {

    AuthToken validateToken(String tenant, String userToken);
    AuthToken validateToken(String tenant, String userToken, Meter calls);

    List<Endpoint> getEndpointsForToken(String userToken);
    List<Endpoint> getEndpointsForToken(String userToken, Meter calls);

    AuthGroups getGroups(String userId);
    AuthGroups getGroups(String userId, Meter calls);

    String getBase64EndpointsStringForHeaders(String userToken, String format);
    String getBase64EndpointsStringForHeaders(String userToken, String format, Meter calls);
}
