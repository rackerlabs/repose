package com.rackspace.lefty.tenant

import org.apache.commons.lang3.RandomStringUtils
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

import static javax.servlet.http.HttpServletResponse.SC_OK
import static org.openrepose.commons.utils.http.CommonHttpHeader.AUTH_TOKEN
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.TENANT_ID

class TenantCullingFilterFunctionalTest extends ReposeValveTest {
    static MockIdentityV2Service fakeIdentityService

    def setupSpec() {
        def params = properties.defaultTemplateParams
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('withKeystone', params)
        repose.start()

        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        fakeIdentityService.checkTokenValid = true
        deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
    }

    def setup() {
        fakeIdentityService.with {
            // This is required to ensure that one piece of the authentication data is changed
            // so that the cached version in the Akka Client is not used.
            client_password = RandomStringUtils.random(8, 'ABCDEFGHIJKLMNOPQRSTUVWYZabcdefghijklmnopqrstuvwyz-_1234567890')
            client_token = UUID.randomUUID().toString()
        }
    }

    def "Sends Tenant that doesn't have role"() {
        given: "a header and corresponding valid token to return from Keystone/Identity"
        def headers = [
                (AUTH_TOKEN): fakeIdentityService.client_token
        ]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then:
        mc.handlings.size() == 1
        mc.receivedResponse.code as Integer == SC_OK
        def tenantIds = mc.handlings[0].request.headers.findAll(TENANT_ID)
        tenantIds.size() == 1
        tenantIds[0] == '123456'
    }
}
