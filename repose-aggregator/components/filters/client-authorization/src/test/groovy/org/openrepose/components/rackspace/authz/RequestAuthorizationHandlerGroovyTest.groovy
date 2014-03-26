package org.openrepose.components.rackspace.authz
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

import static org.mockito.Mockito.mock
import static org.powermock.api.mockito.PowerMockito.when

class RequestAuthorizationHandlerGroovyTest extends Specification {

    AuthenticationService authenticationService
    AuthenticateResponse authenticateResponse
    EndpointListCache endpointListCache
    ServiceEndpoint serviceEndpoint
    ServiceAdminRoles serviceAdminRoles
    FilterDirector filterDirector
    HttpServletRequest httpServletRequest
    RequestAuthorizationHandler requestAuthorizationHandler

    def setup() {
        authenticationService = Mock()
        authenticateResponse = Mock()
        endpointListCache = Mock()
        serviceEndpoint = Mock()
        serviceAdminRoles = Mock()
        filterDirector = new FilterDirectorImpl()
        httpServletRequest = mock(HttpServletRequest.class)
        requestAuthorizationHandler = Mock()
    }

    def "auth should be bypassed if an x-roles header role matches within a configured list of service admin roles"() {
        given:
        when(httpServletRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn("abc")
        when(httpServletRequest.getHeaders(OpenStackServiceHeader.ROLES.toString())).thenReturn(Collections.enumeration(["role0", "role1", "role2"]))
        serviceAdminRoles.getServiceAdminRole() >> new ArrayList<String>()
        serviceAdminRoles.getServiceAdminRole().add("role1")

        requestAuthorizationHandler = new RequestAuthorizationHandler(authenticationService, endpointListCache,
                serviceEndpoint, serviceAdminRoles)

        when:
        requestAuthorizationHandler.authorizeRequest(filterDirector, httpServletRequest)

        then:
        filterDirector.getFilterAction() == FilterAction.PASS
    }

    def "auth should not be bypassed if the x-roles header role does not match within a configured list of service admin roles"() {
        given:
        when(httpServletRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn("abc")
        when(httpServletRequest.getHeaders(OpenStackServiceHeader.ROLES.toString())).thenReturn(Collections.enumeration(["role0", "role2"]))
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
