package features.filters.clientauthn.RackspaceAuthValidUserValidToken

import features.filters.clientauthn.IdentityServiceResponseSimulator
import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import spock.lang.Unroll

class ValidUserValidToken200 extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    static IdentityServiceResponseSimulator fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()

        repose.applyConfigs("/features/filters/clientauthn/RackspaceAuthValidUserValidToken")
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


    def "ValidUserValidToken-200"() {


        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(reposeEndpoint+"/v1/usertest1", 'GET', ['X-Auth-Token': '358484212:10469688' ])

        then: "User should receive a valid token response"
        mc.receivedResponse.code == "200"
    }
}