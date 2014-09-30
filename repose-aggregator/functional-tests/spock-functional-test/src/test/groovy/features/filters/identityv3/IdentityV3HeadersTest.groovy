package features.filters.identityv3

import framework.ReposeValveTest
import framework.mocks.MockIdentityV3Service
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

/**
 * Created by jennyvo on 8/26/14.
 */
class IdentityV3HeadersTest extends ReposeValveTest{
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/common",params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        fakeIdentityV3Service.resetCounts()
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null,fakeIdentityV3Service.handler)
    }

    def cleanupSpec() {
        if(deproxy)
            deproxy.shutdown()
        if(repose)
            repose.stop()
    }

    def "When token is validated, set of headers should be generated"(){
        when: "I send a GET request to Repose with an X-Auth-Token header"
        fakeIdentityV3Service.resetCounts()
        fakeIdentityV3Service.default_region = "DFW"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "Repose should validate the token and path the user's default region as the X-Default_Region header to the origin service"
        mc.receivedResponse.code == "200"
        fakeIdentityV3Service.validateTokenCount == 1
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request = mc.handlings[0].request
        request.headers.contains("X-Default-Region")
        request.headers.contains("X-Authorization")
        request.headers.contains("X-Project-Id")
        request.headers.contains("X-Project-Name")
        request.headers.contains("X-User-Id")
        request.headers.contains("X-User-Name")
        request.headers.contains("X-Roles")
        request.headers.contains("X-pp-user")
        request.headers.contains("X-pp-groups")
        request.headers.contains("X-Token-Expires")
        request.headers.getFirstValue("X-Default-Region") == "DFW"

        when: "I send a second GET request to Repose with the same token"
        fakeIdentityV3Service.resetCounts()
        mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "Repose should use the cache, not call out to the fake identity service, and pass the request to origin service with the same X-Default-Region header"
        mc.receivedResponse.code == "200"
        fakeIdentityV3Service.validateTokenCount == 0
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.contains("X-Default-Region")
        request2.headers.getFirstValue("X-Default-Region") == "DFW"
    }

    def "when client failed to authenticate, the XXX-Authentication header should be expected" () {
        given:
        fakeIdentityV3Service.with {
            client_domainid = 11111
            client_userid = 11111
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        fakeIdentityV3Service.validateTokenHandler = {
            tokenId, request ->
                new Response(404, null, null, fakeIdentityV3Service.identityFailureAuthJsonRespTemplate)
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/11111/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "401"
        mc.receivedResponse.headers.getFirstValue("WWW-Authenticate") == "Keystone uri=http://"+identityEndpoint.hostname+":"+properties.identityPort
    }
}
