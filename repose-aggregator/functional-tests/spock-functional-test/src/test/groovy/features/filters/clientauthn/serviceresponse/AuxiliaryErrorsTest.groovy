package features.filters.clientauthn.serviceresponse

import framework.mocks.MockIdentityService
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

class AuxiliaryErrorsTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    static MockIdentityService fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/zerocachetime", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/connectionpooling", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)

    }

    def setup() {

    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }
        repose.stop()
    }

    @Unroll("Identity Service Broken Admin Call: #adminBroken Broken Token Validation Call: #validateBroken Broken Groups Call: #groupsBroken Error Code: #errorCode")
    def "When the identity service endpoint returns failed or unexpected responses"() {

        given: "When Calls to Auth Return bad responses"

        fakeIdentityService.resetHandlers()
        if (adminBroken) {
            fakeIdentityService.generateTokenHandler = { request, xml -> return new Response(errorCode) }
        }
        if (validateBroken) {
            fakeIdentityService.validateTokenHandler = { tokenId, request, xml -> return new Response(errorCode) }
        }
        if (groupsBroken) {
            fakeIdentityService.getGroupsHandler = { userId, request, xml -> return new Response(errorCode) }
        }

        def tokenId = "${adminBroken} + ${validateBroken} + ${groupsBroken} + ${errorCode}"

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': fakeIdentityService.client_token+tokenId])

        then: "User should receive a " + expectedCode + "response"
        mc.receivedResponse.code == expectedCode

        where:
        adminBroken | validateBroken | groupsBroken | errorCode | expectedCode
        true        | false          | false        | 400       | "500"
        true        | false          | false        | 401       | "500"
        true        | false          | false        | 402       | "500"
        true        | false          | false        | 403       | "500"
        true        | false          | false        | 404       | "500"
        true        | false          | false        | 413       | "500"
        true        | false          | false        | 429       | "500"
        true        | false          | false        | 500       | "500"
        true        | false          | false        | 501       | "500"
        true        | false          | false        | 502       | "500"
        true        | false          | false        | 503       | "500"

        false       | true           | false        | 400       | "500"
        false       | true           | false        | 401       | "500"
        false       | true           | false        | 402       | "500"
        false       | true           | false        | 403       | "500"
        false       | true           | false        | 404       | "401"
        false       | true           | false        | 413       | "500"
        false       | true           | false        | 429       | "500"
        false       | true           | false        | 500       | "500"
        false       | true           | false        | 501       | "500"
        false       | true           | false        | 502       | "500"
        false       | true           | false        | 503       | "500"

        false       | false          | true         | 400       | "500"
        false       | false          | true         | 401       | "500"
        false       | false          | true         | 402       | "500"
        false       | false          | true         | 403       | "500"
        false       | false          | true         | 404       | "500"
        false       | false          | true         | 413       | "500"
        false       | false          | true         | 429       | "500"
        false       | false          | true         | 500       | "500"
        false       | false          | true         | 501       | "500"
        false       | false          | true         | 502       | "500"
        false       | false          | true         | 503       | "500"
    }

}
