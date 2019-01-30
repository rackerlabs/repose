/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package features.recipes.tenantcullingrbac

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.*
import scaffold.category.Recipe
import spock.lang.Shared
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.SC_OK
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import static javax.ws.rs.core.MediaType.APPLICATION_JSON

@Category(Recipe)
class TenantCullingRBACRecipeTest extends ReposeValveTest {
    @Shared
    MockIdentityV2Service fakeIdentityService

    def setupSpec() {
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)

        deproxy = new Deproxy()
        deproxy.addEndpoint(port: properties.identityPort, name: 'identity service', defaultHandler: fakeIdentityService.handler)
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('features/recipes/tenantcullingrbac/common', params)
        repose.start()
    }

    def setup() {
        // Reset the mock Identity service
        fakeIdentityService.resetDefaultParameters()
        fakeIdentityService.resetCounts()
        fakeIdentityService.resetHandlers()
        fakeIdentityService.tokenExpiresAt = null
        fakeIdentityService.checkTokenValid = false

        // Change the client token for each test so that the cached data in the Akka Client is not used.
        fakeIdentityService.client_token = UUID.randomUUID().toString()
    }

    def "happy path: a user makes a valid request that has tenants culled"() {
        given:
        fakeIdentityService.validateTokenHandler = createValidateTokenHandler(
            roles: [[name: 'an:admin', tenantId: 'admin-tenant-id'], [name: 'a:chump', tenantId: 'chump-id']]
        )

        when:
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            method: 'POST',
            path: "/realistic/${fakeIdentityService.client_tenantid}",
            headers: [
                'X-Auth-Token': fakeIdentityService.client_token
            ]
        )

        then: 'the user to origin service interaction is successful'
        messageChain.handlings.size() == 1
        messageChain.receivedResponse.code as Integer == 200

        and: "only the role that granted access to the resource will be present in the X-Relevant-Roles header"
        messageChain.handlings[0].request.headers.findAll('X-Relevant-Roles') == ['an:admin']

        and: "only the tenant-id associated with the admin role will be present in the X-Tenant-Id header"
        messageChain.handlings[0].request.headers.findAll('X-Tenant-Id') == ['admin-tenant-id']

        and: "only the default tenant name will be present in the X-Tenant-Name header"
        messageChain.handlings[0].request.headers.findAll('X-Tenant-Name') == [fakeIdentityService.client_tenantid]

        when: "the x-roles header is parsed at the origin service"
        def roles = messageChain.handlings[0].request.headers.findAll('X-Roles').collect { it.split(',') }.flatten()

        then: "all of the user roles will be present in the X-Roles header"
        roles.size() == 2
        roles.containsAll(['an:admin', 'a:chump'])
    }

    @Unroll
    def "access granted is #accessGranted when a user with roles #userRoles makes a request"() {
        given:
        fakeIdentityService.validateTokenHandler = createValidateTokenHandler(
            roles: userRoles
        )

        when:
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            method: method,
            path: path,
            headers: [
                'X-Auth-Token': fakeIdentityService.client_token
            ]
        )

        then: 'the user to origin service interaction is as expected'
        messageChain.handlings.size() == (accessGranted ? 1 : 0)
        messageChain.receivedResponse.code as Integer == expectedStatus

        where:
        accessGranted | userRoles        | method   | path          | expectedStatus
        true          | [[name: 'get']]  | 'GET'    | '/access'     | 200
        false         | [[name: 'get']]  | 'POST'   | '/access'     | 403
        false         | [[name: 'get']]  | 'DELETE' | '/access'     | 405
        false         | [[name: 'post']] | 'GET'    | '/access'     | 403
        true          | [[name: 'post']] | 'POST'   | '/access'     | 200
        false         | [[name: 'post']] | 'DELETE' | '/access'     | 405
        false         | [[name: 'get']]  | 'GET'    | '/access/dne' | 404
        false         | [[name: 'get']]  | 'POST'   | '/access/dne' | 404
        false         | [[name: 'get']]  | 'DELETE' | '/access/dne' | 404
        false         | [[name: 'post']] | 'GET'    | '/access/dne' | 404
        false         | [[name: 'post']] | 'POST'   | '/access/dne' | 404
        false         | [[name: 'post']] | 'DELETE' | '/access/dne' | 404
    }

    @Unroll
    def "tenants are culled when a user with roles #userRoles and default tenant #defaultTenantId makes a request"() {
        given:
        fakeIdentityService.validateTokenHandler = createValidateTokenHandler(
            tenantId: defaultTenantId,
            roles: userRoles
        )

        when:
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            method: 'GET',
            path: '/culling',
            headers: [
                'X-Auth-Token': fakeIdentityService.client_token
            ]
        )

        then: "the user to origin service interaction is successful"
        messageChain.handlings.size() == 1
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "all of the user roles will be present in the X-Roles header"

        and: "only any tenant ID associated with a relevant role will be present in the X-Tenant-Id header"
        messageChain.handlings[0].request.headers.findAll('X-Tenant-Id').collect({it.split(',')}).flatten() == relevantRoles.collect {
            it.tenantId
        } - null

        and: "only the default tenant name will be present in the X-Tenant-Name header"
        messageChain.handlings[0].request.headers.findAll('X-Tenant-Name') == [defaultTenantId] - null

        when: "the x-relevant-roles header is parsed at the origin service"
        def relRoles = messageChain.handlings[0].request.headers.findAll('X-Relevant-Roles').collect { it.split(',') }.flatten()

        then: "all of the user roles will be present in the X-Roles header"
        relRoles.size() == relevantRoles.size()
        relRoles.containsAll(relevantRoles.collect { it.name })

        when: "the x-roles header is parsed at the origin service"
        def roles = messageChain.handlings[0].request.headers.findAll('X-Roles').collect { it.split(',') }.flatten()

        then: "all of the user roles will be present in the X-Roles header"
        roles.size() == userRoles.size()
        roles.containsAll(userRoles.collect { it.name })

        where:
        userRoles                                                                                                                 | defaultTenantId | relevantRoles
        [[name: 'get']]                                                                                                           | null            | [[name: 'get']]
        [[name: 'get']]                                                                                                           | 'def-ten-id'    | [[name: 'get']]
        [[name: 'get', tenantId: 'get-ten-id']]                                                                                   | null            | [[name: 'get', tenantId: 'get-ten-id']]
        [[name: 'get', tenantId: 'get-ten-id']]                                                                                   | 'def-ten-id'    | [[name: 'get', tenantId: 'get-ten-id']]
        [[name: 'admin']]                                                                                                         | null            | [[name: 'admin']]
        [[name: 'admin']]                                                                                                         | 'def-ten-id'    | [[name: 'admin']]
        [[name: 'admin', tenantId: 'admin-ten-id']]                                                                               | null            | [[name: 'admin', tenantId: 'admin-ten-id']]
        [[name: 'admin', tenantId: 'admin-ten-id']]                                                                               | 'def-ten-id'    | [[name: 'admin', tenantId: 'admin-ten-id']]
        [[name: 'get'], [name: 'admin']]                                                                                          | null            | [[name: 'get'], [name: 'admin']]
        [[name: 'get'], [name: 'admin']]                                                                                          | 'def-ten-id'    | [[name: 'get'], [name: 'admin']]
        [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin']]                                                                  | null            | [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin']]
        [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin']]                                                                  | 'def-ten-id'    | [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin']]
        [[name: 'get'], [name: 'admin', tenantId: 'admin-ten-id']]                                                                | null            | [[name: 'get'], [name: 'admin', tenantId: 'admin-ten-id']]
        [[name: 'get'], [name: 'admin', tenantId: 'admin-ten-id']]                                                                | 'def-ten-id'    | [[name: 'get'], [name: 'admin', tenantId: 'admin-ten-id']]
        [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin', tenantId: 'admin-ten-id']]                                        | null            | [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin', tenantId: 'admin-ten-id']]
        [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin', tenantId: 'admin-ten-id']]                                        | 'def-ten-id'    | [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin', tenantId: 'admin-ten-id']]
        [[name: 'get'], [name: 'foo']]                                                                                            | null            | [[name: 'get']]
        [[name: 'get'], [name: 'foo']]                                                                                            | 'def-ten-id'    | [[name: 'get']]
        [[name: 'get', tenantId: 'get-ten-id'], [name: 'foo']]                                                                    | null            | [[name: 'get', tenantId: 'get-ten-id']]
        [[name: 'get', tenantId: 'get-ten-id'], [name: 'foo']]                                                                    | 'def-ten-id'    | [[name: 'get', tenantId: 'get-ten-id']]
        [[name: 'admin'], [name: 'foo']]                                                                                          | null            | [[name: 'admin']]
        [[name: 'admin'], [name: 'foo']]                                                                                          | 'def-ten-id'    | [[name: 'admin']]
        [[name: 'admin', tenantId: 'admin-ten-id'], [name: 'foo']]                                                                | null            | [[name: 'admin', tenantId: 'admin-ten-id']]
        [[name: 'admin', tenantId: 'admin-ten-id'], [name: 'foo']]                                                                | 'def-ten-id'    | [[name: 'admin', tenantId: 'admin-ten-id']]
        [[name: 'get'], [name: 'admin'], [name: 'foo']]                                                                           | null            | [[name: 'get'], [name: 'admin']]
        [[name: 'get'], [name: 'admin'], [name: 'foo']]                                                                           | 'def-ten-id'    | [[name: 'get'], [name: 'admin']]
        [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin'], [name: 'foo']]                                                   | null            | [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin']]
        [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin'], [name: 'foo']]                                                   | 'def-ten-id'    | [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin']]
        [[name: 'get'], [name: 'admin', tenantId: 'admin-ten-id'], [name: 'foo']]                                                 | null            | [[name: 'get'], [name: 'admin', tenantId: 'admin-ten-id']]
        [[name: 'get'], [name: 'admin', tenantId: 'admin-ten-id'], [name: 'foo']]                                                 | 'def-ten-id'    | [[name: 'get'], [name: 'admin', tenantId: 'admin-ten-id']]
        [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin', tenantId: 'admin-ten-id'], [name: 'foo']]                         | null            | [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin', tenantId: 'admin-ten-id']]
        [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin', tenantId: 'admin-ten-id'], [name: 'foo']]                         | 'def-ten-id'    | [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin', tenantId: 'admin-ten-id']]
        [[name: 'get'], [name: 'foo', tenantId: 'foo-ten-id']]                                                                    | null            | [[name: 'get']]
        [[name: 'get'], [name: 'foo', tenantId: 'foo-ten-id']]                                                                    | 'def-ten-id'    | [[name: 'get']]
        [[name: 'get', tenantId: 'get-ten-id'], [name: 'foo', tenantId: 'foo-ten-id']]                                            | null            | [[name: 'get', tenantId: 'get-ten-id']]
        [[name: 'get', tenantId: 'get-ten-id'], [name: 'foo', tenantId: 'foo-ten-id']]                                            | 'def-ten-id'    | [[name: 'get', tenantId: 'get-ten-id']]
        [[name: 'admin'], [name: 'foo', tenantId: 'foo-ten-id']]                                                                  | null            | [[name: 'admin']]
        [[name: 'admin'], [name: 'foo', tenantId: 'foo-ten-id']]                                                                  | 'def-ten-id'    | [[name: 'admin']]
        [[name: 'admin', tenantId: 'admin-ten-id'], [name: 'foo', tenantId: 'foo-ten-id']]                                        | null            | [[name: 'admin', tenantId: 'admin-ten-id']]
        [[name: 'admin', tenantId: 'admin-ten-id'], [name: 'foo', tenantId: 'foo-ten-id']]                                        | 'def-ten-id'    | [[name: 'admin', tenantId: 'admin-ten-id']]
        [[name: 'get'], [name: 'admin'], [name: 'foo', tenantId: 'foo-ten-id']]                                                   | null            | [[name: 'get'], [name: 'admin']]
        [[name: 'get'], [name: 'admin'], [name: 'foo', tenantId: 'foo-ten-id']]                                                   | 'def-ten-id'    | [[name: 'get'], [name: 'admin']]
        [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin'], [name: 'foo', tenantId: 'foo-ten-id']]                           | null            | [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin']]
        [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin'], [name: 'foo', tenantId: 'foo-ten-id']]                           | 'def-ten-id'    | [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin']]
        [[name: 'get'], [name: 'admin', tenantId: 'admin-ten-id'], [name: 'foo', tenantId: 'foo-ten-id']]                         | null            | [[name: 'get'], [name: 'admin', tenantId: 'admin-ten-id']]
        [[name: 'get'], [name: 'admin', tenantId: 'admin-ten-id'], [name: 'foo', tenantId: 'foo-ten-id']]                         | 'def-ten-id'    | [[name: 'get'], [name: 'admin', tenantId: 'admin-ten-id']]
        [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin', tenantId: 'admin-ten-id'], [name: 'foo', tenantId: 'foo-ten-id']] | null            | [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin', tenantId: 'admin-ten-id']]
        [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin', tenantId: 'admin-ten-id'], [name: 'foo', tenantId: 'foo-ten-id']] | 'def-ten-id'    | [[name: 'get', tenantId: 'get-ten-id'], [name: 'admin', tenantId: 'admin-ten-id']]
    }

    @Unroll
    def "#headerName should be #behavior from the request to the origin service when provided by the user on the request"() {
        given:
        fakeIdentityService.validateTokenHandler = createValidateTokenHandler(
            tenantId: null,
            roles: [[name: 'test-role']],
            region: 'test-region'
        )

        when:
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            method: 'GET',
            path: '/open',
            headers: [
                new Header('X-Auth-Token', fakeIdentityService.client_token),
            ] + providedValues.collect { new Header(headerName, it) }
        )

        then: 'the user to origin service interaction is successful'
        messageChain.handlings.size() == 1
        messageChain.receivedResponse.code as Integer == SC_OK

        and: 'the header in question has the expected values'
        messageChain.handlings[0].request.headers.findAll(headerName) == expectedValues

        where:
        headerName             | behavior   | providedValues  | expectedValues
        'X-Authorization'      | 'replaced' | ['Banana']      | ['Proxy']
        'X-Identity-Status'    | 'removed'  | ['Apple']       | []
        'X-User-Name'          | 'replaced' | ['Pear']        | [fakeIdentityService.client_username]
        'X-User-ID'            | 'replaced' | ['Peach']       | [fakeIdentityService.client_userid]
        'X-Authenticated-By'   | 'removed'  | ['Grape']       | []
        'X-Roles'              | 'replaced' | ['Dragonfruit'] | ['test-role']
        'X-PP-User'            | 'replaced' | ['Pineapple']   | [fakeIdentityService.client_username]
        'X-PP-Groups'          | 'replaced' | ['Strawberry']  | []
        'X-Catalog'            | 'removed'  | ['Orange']      | []
        'X-Impersonator-ID'    | 'removed'  | ['Cherry']      | []
        'X-Impersonator-Name'  | 'removed'  | ['Kiwi']        | []
        'X-Impersonator-Roles' | 'removed'  | ['Mango']       | []
        'X-Contact-ID'         | 'removed'  | ['Watermelon']  | []
        'X-Default-Region'     | 'replaced' | ['Lemon']       | ['test-region']
        'X-Tenant-ID'          | 'removed'  | ['Papaya']      | []
        'X-Tenant-Name'        | 'removed'  | ['Apricot']     | []
        'X-Relevant-Roles'     | 'removed'  | ['Cranberry']   | []
    }

    def "the X-Auth-Token-Key request header is overridden and sent to the origin service"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            method: 'GET',
            path: '/open',
            headers: [
                'X-Auth-Token'    : fakeIdentityService.client_token,
                'X-Auth-Token-Key': 'Tangerine'
            ]
        )

        then: 'the user to origin service interaction is successful'
        messageChain.handlings.size() == 1
        messageChain.receivedResponse.code as Integer == SC_OK

        and: 'the header in question has the expected values'
        messageChain.handlings[0].request.headers.findAll('X-Auth-Token-Key').size() == 1
        messageChain.handlings[0].request.headers.findAll('X-Auth-Token-Key').find { it ==~ 'IDENTITY:V2:TOKEN:.+' }
    }

    def "the X-Token-Expires request header is overridden and sent to the origin service"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            method: 'GET',
            path: '/open',
            headers: [
                'X-Auth-Token'    : fakeIdentityService.client_token,
                'X-Auth-Token-Key': 'Fig'
            ]
        )

        then: 'the user to origin service interaction is successful'
        messageChain.handlings.size() == 1
        messageChain.receivedResponse.code as Integer == SC_OK

        and: 'the header in question has the expected values'
        messageChain.handlings[0].request.headers.findAll('X-Token-Expires').size() == 1
        messageChain.handlings[0].request.headers.findAll('X-Token-Expires').find { it ==~ '\\w+, \\d.*' }
    }

    Closure<Response> createValidateTokenHandler(Map params = [:]) {
        { String tokenId, String tenantId, Request request ->
            def headers = [(CONTENT_TYPE): APPLICATION_JSON]
            def body = fakeIdentityService.createAccessJsonWithValues(params)
            new Response(SC_OK, null, headers, body)
        }
    }
}
