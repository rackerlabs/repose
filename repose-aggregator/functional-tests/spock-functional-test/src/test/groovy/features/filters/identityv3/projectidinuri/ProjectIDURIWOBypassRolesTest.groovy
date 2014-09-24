package features.filters.identityv3.projectidinuri

import framework.ReposeValveTest
import framework.mocks.MockIdentityV3Service
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

class ProjectIDURIWOBypassRolesTest extends ReposeValveTest{
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/common",params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/projectidinuri/noserviceroles", params)
        repose.start()
        waitUntilReadyToServiceRequests('401')

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)
    }

    def cleanupSpec() {
        if(deproxy)
            deproxy.shutdown()
        if(repose)
            repose.stop()
    }

    def setup(){
        fakeIdentityV3Service.resetHandlers()
    }


    @Unroll("Given project ID: #requestProject, response project ID: #responseProject, identity resp code: #authResponseCode, identity group resp code: #groupResponseCode, return #responseCode")
    def "when authenticating project id and without service-admin - fail"() {

        given:
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_projectid = responseProject
            client_userid = requestProject
            service_admin_role = "not-admin"
        }

        if(authResponseCode != 200){
            fakeIdentityV3Service.validateTokenHandler = {
                tokenId, request ->
                    new Response(authResponseCode)
            }
        }

        if(groupResponseCode != 200){
            fakeIdentityV3Service.getGroupsHandler = {
                userId, request ->
                    new Response(groupResponseCode)
            }
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestProject/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 0

        where:
        requestProject | responseProject  | authResponseCode | groupResponseCode | x_www_auth | responseCode
        813            | 813              | 500              | 200               | false      | "500"
        814            | 814              | 404              | 200               | true       | "401"
        815            | 815              | 200              | 404               | false      | "500"
        816            | 816              | 200              | 500               | false      | "500"
        811            | 812              | 200              | 200               | true       | "401"


    }

    def "When authenticating a valid project id and without bypass roles, repose should return a 200"() {
        given:
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_projectid = 999
            client_userid = 999
            service_admin_role = "non-admin"
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url:"$reposeEndpoint/servers/999/",
                method:'GET',
                headers:['content-type': 'application/json', 'X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.contains("x-subject-token")
        !request2.headers.contains("x-identity-status")
        request2.headers.contains("x-authorization")
        request2.headers.getFirstValue("x-authorization") == "Proxy"
    }

}
