package com.rackspace.lefty.tenant

import groovy.json.JsonBuilder
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import spock.lang.Shared
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.SC_OK
import static org.openrepose.commons.utils.http.CommonHttpHeader.AUTH_TOKEN
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.TENANT_ID

class TenantCullingFilterFunctionalTest extends ReposeValveTest {
    static MockIdentityV2Service fakeIdentityService

    @Shared
    def roleNameOne = 'role-name-one'
    @Shared
    def roleNameTwo = 'role-name-two'
    @Shared
    def tenantIdDef = 'tenant-id-default'
    @Shared
    def tenantIdOne = 'tenant-id-one'
    @Shared
    def tenantIdTwo = 'tenant-id-two'
    @Shared
    def oneRole = [[name: roleNameOne, tenantId: tenantIdOne]]
    @Shared
    def twoRoles = oneRole + [[name: roleNameTwo, tenantId: tenantIdTwo]]
    @Shared
    def sameRoles = oneRole + [[name: roleNameOne, tenantId: tenantIdTwo]]

    def setupSpec() {
        def params = properties.defaultTemplateParams
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort as Integer)
        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('withKeystone', params)
        repose.start()

        fakeIdentityService = new MockIdentityV2Service(properties.identityPort as Integer, properties.targetPort as Integer)
        fakeIdentityService.checkTokenValid = true
        deproxy.addEndpoint(properties.identityPort as Integer, 'identity service', null, fakeIdentityService.handler)
    }

    def setup() {
        fakeIdentityService.with {
            // This is required to ensure that one piece of the authentication data is changed
            // so that the cached version in the Akka Client is not used.
            client_token = UUID.randomUUID().toString()
        }
    }

    def "Sends default Tenant"() {
        given: "a configured Keystone/Identity and appropriate request header"
        fakeIdentityService.with {
            client_tenantid = UUID.randomUUID().toString()
        }
        def headers = [
                (AUTH_TOKEN): fakeIdentityService.client_token
        ]

        when: "the request is made"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "the origin service should receive the default tenant"
        mc.handlings.size() == 1
        mc.receivedResponse.code as Integer == SC_OK
        def tenantIds = mc.handlings[0].request.headers.findAll(TENANT_ID)
        tenantIds.size() == 1
        tenantIds.contains(fakeIdentityService.client_tenantid)
    }

    @Unroll
    def "#testName"() {
        given: "a configured Keystone/Identity and appropriate request headers"
        fakeIdentityService.with {
            client_tenantid = clientTenantId
            validateTokenHandler = createValidateTokenHandler(roles)
        }
        def headers = [
                (AUTH_TOKEN)      : fakeIdentityService.client_token,
                'X-Relevant-Roles': relevant
        ]

        when: "the request is made"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "the origin service should receive the appropriate tenant(s)"
        mc.handlings.size() == 1
        mc.receivedResponse.code as Integer == SC_OK
        def tenantIds = mc.handlings[0].request.headers.findAll(TENANT_ID)
        tenantIds.size() == expected.size()
        tenantIds.containsAll(expected)
        tenantIds.disjoint(notExpected)

        where:
        testName                                                       | clientTenantId | roles              | relevant                    | expected                                | notExpected
        "Sends Tenant that matches a role"                             | null           | [roles: oneRole]   | roleNameOne                 | [tenantIdOne]                           | []
        "Sends Default and Tenant that matches a role"                 | tenantIdDef    | [roles: oneRole]   | roleNameOne                 | [tenantIdDef, tenantIdOne]              | []
        "Sends only Default with role mismatch"                        | tenantIdDef    | [roles: oneRole]   | roleNameTwo                 | [tenantIdDef]                           | [tenantIdOne]
        "Sends Tenant that matches single role"                        | null           | [roles: twoRoles]  | roleNameOne                 | [tenantIdOne]                           | [tenantIdTwo]
        "Sends multiple Tenants that match single role"                | null           | [roles: sameRoles] | roleNameOne                 | [tenantIdOne, tenantIdTwo]              | []
        "Sends multiple Tenants that match multiple roles"             | null           | [roles: twoRoles]  | "$roleNameOne,$roleNameTwo" | [tenantIdOne, tenantIdTwo]              | []
        "Sends Tenants with and without roles"                         | tenantIdDef    | [roles: twoRoles]  | roleNameOne                 | [tenantIdDef, tenantIdOne]              | [tenantIdTwo]
        "Sends Default and multiple Tenants that match single role"    | tenantIdDef    | [roles: sameRoles] | roleNameOne                 | [tenantIdDef, tenantIdOne, tenantIdTwo] | []
        "Sends Default and multiple Tenants that match multiple roles" | tenantIdDef    | [roles: twoRoles]  | "$roleNameOne,$roleNameTwo" | [tenantIdDef, tenantIdOne, tenantIdTwo] | []
    }

    def "Replaces Tenant"() {
        given: "a configured Keystone/Identity and appropriate request headers"
        fakeIdentityService.with {
            client_tenantid = UUID.randomUUID().toString()
        }
        def headers = [
                (AUTH_TOKEN): fakeIdentityService.client_token,
                (TENANT_ID) : tenantIdOne
        ]

        when: "the request is made"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "the origin service should receive only the default tenant"
        mc.handlings.size() == 1
        mc.receivedResponse.code as Integer == SC_OK
        def tenantIds = mc.handlings[0].request.headers.findAll(TENANT_ID)
        tenantIds.size() == 1
        tenantIds.contains(fakeIdentityService.client_tenantid)
        !tenantIds.contains(tenantIdOne)
    }

    Closure<Response> createValidateTokenHandler(Map params) {
        { String tokenId, String tenantId, Request request ->
            def headers = ["Content-type": "application/json"]
            // TODO: The method in Repose's MockIdentityV2Service should be used when updated.
            //def body = fakeIdentityService.createAccessJsonWithValues(params)
            def body = createAccessJsonWithValues(params)
            new Response(SC_OK, null, headers, body)
        }
    }

    // TODO: This method has been updated in Repose's MockIdentityV2Service, but wasn't released at the time of this writing.
    String createAccessJsonWithValues(Map values = [:]) {
        def token = values.token ?: fakeIdentityService.client_token
        def expires = values.expires ?: fakeIdentityService.getExpires()
        def tenantId = values.tenantId ?: fakeIdentityService.client_tenantid
        def userId = values.userId ?: fakeIdentityService.client_userid
        def username = values.username ?: fakeIdentityService.client_username
        def roles = (values.roles ?: [[name: "identity:admin"]])

        def json = new JsonBuilder()

        json {
            access {
                delegate.token {
                    id token
                    delegate.expires expires
                    if (tenantId) {
                        tenant {
                            id tenantId
                            name tenantId
                        }
                    }
                    if (values.authBy) {
                        'RAX-AUTH:authenticatedBy' values.authBy
                    }
                }
                user {
                    id userId
                    name username
                    'RAX-AUTH:defaultRegion' "the-default-region"
                    delegate.roles roles.withIndex(1).collect { role, index ->
                        [name: role.name, id: index] + (role.tenantId ? [tenantId: role.tenantId] : [:])
                    }
                }
                serviceCatalog([
                        {
                            name "cloudServersOpenStack"
                            type "compute"
                            endpoints([
                                    {
                                        publicURL "https://ord.servers.api.rackspacecloud.com/v2/$tenantId"
                                        delegate.region "ORD"
                                        delegate.tenantId tenantId
                                        versionId "2"
                                        versionInfo "https://ord.servers.api.rackspacecloud.com/v2"
                                        versionList "https://ord.servers.api.rackspacecloud.com/"
                                    }
                            ])
                        }
                ])
            }
        }
        json.toString()
    }
}
