package features.filters.clientauthz.serviceresponse

import features.filters.clientauthn.IdentityServiceResponseSimulator
import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain

class ServiceListFeatureTest extends ReposeValveTest {

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

    def "When user requests a URL that is not in the user's service list should receive a 403 FORBIDDEN response"(){

        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        def token = UUID.randomUUID().toString()
        fakeIdentityService.client_token = token
        fakeIdentityService.origin_service_port = 99999

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/v1/"+token+"/ss", 'GET', ['X-Auth-Token': token])
        def foundLogs = reposeLogSearch.searchByString("User token: " + token +
                ": The user's service catalog does not contain an endpoint that matches the endpoint configured in openstack-authorization.cfg.xml")

        then: "User should receive a 403 FORBIDDEN response"
        foundLogs.size() == 1
        mc.handlings.size() == 0
        mc.receivedResponse.code == "403"
    }

}
