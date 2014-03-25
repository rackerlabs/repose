package features.filters.clientauthz.serviceresponse

import framework.mocks.MockIdentityService
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class ServiceListFeatureTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    static MockIdentityService fakeIdentityService

    def setupSpec() {
        cleanLogDirectory()

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthz/servicelist", params)
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

    def "user requests a URL that is in the user's service list"() {
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url:reposeEndpoint + "/v1/"+fakeIdentityService.client_tenant+"/ss", method:'GET',
                headers:['X-Auth-Token': fakeIdentityService.client_token])

        then: "User should receive a 200 response"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }


    def "D-14988: client auth config should work without service-role element"() {
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url:reposeEndpoint + "/v1/"+fakeIdentityService.client_tenant+"/ss", method:'GET', headers:['X-Auth-Token': fakeIdentityService.client_token])

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
        fakeIdentityService.originServicePort = 99999

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url:reposeEndpoint + "/v1/"+token+"/ss", method:'GET', headers:['X-Auth-Token': token])
        def foundLogs = reposeLogSearch.searchByString("User token: " + token +
                ": The user's service catalog does not contain an endpoint that matches the endpoint configured in openstack-authorization.cfg.xml")

        then: "User should receive a 403 FORBIDDEN response"
        foundLogs.size() == 1
        mc.handlings.size() == 0
        mc.receivedResponse.code == "403"
    }

}
