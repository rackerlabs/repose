package org.openrepose.components.rackspace.authz
import com.rackspace.auth.AuthToken
import com.rackspace.auth.openstack.AuthenticationService
import com.rackspace.papi.commons.util.http.CommonHttpHeader
import com.rackspace.papi.commons.util.http.OpenStackServiceHeader
import com.rackspace.papi.filter.logic.FilterAction
import com.rackspace.papi.filter.logic.FilterDirector
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl
import org.openrepose.components.authz.rackspace.config.ServiceAdminRoles
import org.openrepose.components.authz.rackspace.config.ServiceEndpoint
import org.openrepose.components.rackspace.authz.cache.EndpointListCache
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

class RequestAuthorizationHandlerGroovyTest extends Specification {

    AuthenticationService authenticationService
    AuthenticateResponse authenticateResponse
    EndpointListCache endpointListCache
    ServiceEndpoint serviceEndpoint
    ServiceAdminRoles serviceAdminRoles
    FilterDirector filterDirector
    HttpServletRequest httpServletRequest
    RequestAuthorizationHandler requestAuthorizationHandler
    AuthToken authToken

    def setup() {
        authenticationService = Mock()
        authenticateResponse = Mock()
        endpointListCache = Mock()
        serviceEndpoint = Mock()
        serviceAdminRoles = Mock()
        filterDirector = new FilterDirectorImpl()
        httpServletRequest = Mock()
        requestAuthorizationHandler = Mock()
        authToken = Mock()
    }

    def "auth should be bypassed if an x-roles header role matches within a configured list of service admin roles"() {
        given:
        httpServletRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString()) >> "abc"
        httpServletRequest.getHeader(OpenStackServiceHeader.ROLES.toString()) >> "role0,role1,role2"
        serviceAdminRoles.getServiceAdminRole() >> new ArrayList<String>()
        serviceAdminRoles.getServiceAdminRole().add("role1")

        requestAuthorizationHandler = new RequestAuthorizationHandler(authenticationService, endpointListCache,
                serviceEndpoint, serviceAdminRoles)

        when:
        requestAuthorizationHandler.authorizeRequest(filterDirector, httpServletRequest)

        then:
        filterDirector.getFilterAction() == FilterAction.PASS
    }

    def "auth should be bypassed if a role from token validation matches within a configured list of service admin roles"() {
        when:
        String goodToken = "good_token"
        String role1 = "role1"
        List<String> role1InList = new ArrayList<String>()
        role1InList.add(role1)
        serviceAdminRoles.getServiceAdminRole() >> role1InList
        authenticationService.validateToken(null, goodToken) >> authToken
        authToken.getRoles() >> "role0,role1"

        requestAuthorizationHandler = new RequestAuthorizationHandler(authenticationService, endpointListCache,
                serviceEndpoint, serviceAdminRoles)

        then:
        requestAuthorizationHandler.serviceAdminRolePresent(null, goodToken) == true
    }

    def "auth should not be bypassed if neither an x-roles header role nor a role from token validation matches within a configured list of service admin roles"() {
        given:
        httpServletRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString()) >> "bad_token"
        httpServletRequest.getHeader(OpenStackServiceHeader.ROLES.toString()) >> "role0,role2"
        serviceAdminRoles.getServiceAdminRole() >> new ArrayList<String>()
        serviceAdminRoles.getServiceAdminRole().add("role1")

        requestAuthorizationHandler = new RequestAuthorizationHandler(authenticationService, endpointListCache,
                serviceEndpoint, serviceAdminRoles)

        when:
        requestAuthorizationHandler.authorizeRequest(filterDirector, httpServletRequest)

        then:
        filterDirector.getFilterAction() != FilterAction.PASS
    }
}
