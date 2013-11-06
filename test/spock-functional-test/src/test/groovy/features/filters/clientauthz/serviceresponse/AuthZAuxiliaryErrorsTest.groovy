package features.filters.clientauthz.serviceresponse

import features.filters.clientauthn.IdentityServiceResponseSimulator
import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import spock.lang.Unroll

@Category(Slow.class)
class AuthZAuxiliaryErrorsTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    static IdentityServiceResponseSimulator fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthz/common")
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.getProperty("target.port").toInteger(), 'origin service')
        fakeIdentityService = new IdentityServiceResponseSimulator()
        identityEndpoint = deproxy.addEndpoint(properties.getProperty("identity.port").toInteger(),
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
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.isGetAdminTokenBroken = adminBroken
        fakeIdentityService.errorCode = errorCode
        fakeIdentityService.isGetEndpointsBroken = endpointsBroken

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(reposeEndpoint, 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

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
