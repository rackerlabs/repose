package features.filters.clientauthz.serviceresponse

import features.filters.clientauthn.IdentityServiceResponseSimulator
import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain

/**
 */
class ServiceListFeature extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    static IdentityServiceResponseSimulator fakeIdentityService

    def setupSpec() {
        cleanLogDirectory()

        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthz/servicelist")
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

    def "user requests a URL that is in the user's service list"() {
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/v1/"+fakeIdentityService.client_tenant+"/ss", 'GET',
                ['X-Auth-Token': fakeIdentityService.client_token])

        then: "User should receive a 200 response"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }

    def "user requests a URL that is not in the user's service list"() {
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/v1/" + fakeIdentityService.client_tenant+"/ssnotexists", 'GET',
                ['X-Auth-Token': fakeIdentityService.client_token])

        then: "User should receive a 403 response"
        mc.receivedResponse.code == "403"

        and: "The request does not get forwarded to the origin service"
        mc.handlings.size() == 0
    }

    def "D-14988: client auth config should work without service-role element"() {
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/v1/"+fakeIdentityService.client_tenant+"/ss", 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "No NullPointerException is logged"
        List<String> logs = reposeLogSearch.searchByString("NullPointerException")
        logs.size() == 0

        and: "User should receive a 200 response"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }

}
