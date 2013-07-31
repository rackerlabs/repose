package features.filters.clientauthz.serviceresponse

import features.filters.clientauthn.IdentityServiceResponseSimulator
import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain


class ClientAuthZTest extends ReposeValveTest {

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


    def "When user is authorized should forward request to origin service"(){

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(reposeEndpoint, 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "User should receive a 200 response"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

    }


    def "When user is not authorized should receive a 403 FORBIDDEN response"(){

        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        def token = UUID.randomUUID().toString()
        fakeIdentityService.client_token = token
        fakeIdentityService.origin_service_port = 99999

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(reposeEndpoint, 'GET', ['X-Auth-Token': token])
        def foundLogs = reposeLogSearch.searchByString("User token: " + token +
                ": The user's service catalog does not contain an endpoint that matches the endpoint configured in openstack-authorization.cfg.xml")

        then: "User should receive a 403 FORBIDDEN response"
        foundLogs.size() == 1
        mc.handlings.size() == 0
        mc.receivedResponse.code == "403"
    }

}