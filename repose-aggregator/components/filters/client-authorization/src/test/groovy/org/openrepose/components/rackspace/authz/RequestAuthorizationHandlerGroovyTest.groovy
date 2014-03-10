package org.openrepose.components.rackspace.authz

import com.rackspace.auth.openstack.AuthenticationService
import org.openrepose.components.authz.rackspace.config.ServiceAdminRoles
import org.openrepose.components.authz.rackspace.config.ServiceEndpoint
import org.openrepose.components.rackspace.authz.cache.EndpointListCache
import spock.lang.Specification

class RequestAuthorizationHandlerGroovyTest extends Specification {

    AuthenticationService authenticationService = Mock()
    EndpointListCache endpointListCache = Mock()
    ServiceEndpoint serviceEndpoint = Mock()
    ServiceAdminRoles serviceAdminRoles = Mock()

    RequestAuthorizationHandler requestAuthorizationHandler = new RequestAuthorizationHandler(authenticationService,
            endpointListCache, serviceEndpoint, serviceAdminRoles)

    def "auth should be bypassed if a role matches within a configured list of service admin roles" () {
        requestAuthorizationHandler.authorizeRequest()
    }
}
