package features.filters.clientauthz.serviceresponse

import framework.mocks.MockIdentityService
import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

@Category(Slow.class)
class AuthZAuxiliaryErrorsTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    static MockIdentityService fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthz/common", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }
        repose.stop()
    }

    @Unroll("Identity Service Broken Admin Call: #adminBroken Broken Token Endpoints Call: #endpointsBroken Error Code: #errorCode")
    def "When Auxiliary service is broken for Service Endpoints call"(){

        given: "When Calls to Auth Return bad responses"

        def clientToken = UUID.randomUUID().toString()

        fakeIdentityService.resetHandlers()
        if (adminBroken) {
            fakeIdentityService.generateTokenHandler = { request, xml -> return new Response(errorCode) }
        }
        if (endpointsBroken) {
            fakeIdentityService.getEndpointsHandler = { tokenId, request, xml -> return new Response(errorCode) }
        }

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url:reposeEndpoint, method:'GET', headers:['X-Auth-Token': fakeIdentityService.client_token])

        then: "User should receive a " + expectedCode + "response"
        mc.receivedResponse.code == expectedCode

        where:
        adminBroken | endpointsBroken | errorCode | expectedCode
        true        | false          | 400       | "500"
        true        | false          | 401       | "500"
        true        | false          | 402       | "500"
        true        | false          | 403       | "500"
        true        | false          | 404       | "500"
        true        | false          | 413       | "500"
        true        | false          | 429       | "500"
        true        | false          | 500       | "500"
        true        | false          | 501       | "500"
        true        | false          | 502       | "500"
        true        | false          | 503       | "500"

        false       | true           | 400       | "500"
        false       | true           | 401       | "500"
        false       | true           | 402       | "500"
        false       | true           | 403       | "500"
        false       | true           | 404       | "500"
        false       | true           | 413       | "500"
        false       | true           | 429       | "500"
        false       | true           | 500       | "500"
        false       | true           | 501       | "500"
        false       | true           | 502       | "500"
        false       | true           | 503       | "500"

    }

}
