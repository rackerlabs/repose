package features.filters.keystonev3

import framework.ReposeValveTest
import framework.mocks.MockKeystoneV3Service
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Ignore
import spock.lang.Unroll

/**
 * Created by jennyvo on 8/26/14.
 * Test forward-unauthorized-requests option
 * Acceptance Criteria
 * - When a request is unverified set X-Identity-Status: Indeterminate
 */
class ForwardUnauthorizedReqTest extends ReposeValveTest{

    def static originEndpoint
    def static identityEndpoint

    def static MockKeystoneV3Service fakeKeystoneV3Service

    def setupSpec() {

        deproxy = new Deproxy()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeKeystoneV3Service = new MockKeystoneV3Service(properties.identityPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeKeystoneV3Service.handler)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev3", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev3/forwardunauthorizedrequests", params)
        repose.start()
        waitUntilReadyToServiceRequests('200')
    }

    def cleanupSpec() {
        if(deproxy)
            deproxy.shutdown()
        if(repose)
            repose.stop()
    }

    def setup(){
        sleep(500)
        fakeKeystoneV3Service.resetHandlers()
    }

    def "when send req without credential with forward-unauthorized-request true"() {

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/123456/",
                method: 'GET',
                headers: ['content-type': 'application/json'])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-authorization")
        mc.handlings[0].request.headers.getFirstValue("X-Identity-Status") == "Indeterminate"
    }

    @Unroll("#authResponseCode, #responseCode")
    def "when send req with unauthorized user with forward-unauthorized-request true"() {
        fakeKeystoneV3Service.with {
            client_token = UUID.randomUUID()
            tokenExpiresAt = (new DateTime()).plusDays(1);
            client_projectid = reqProject
            service_admin_role = "not-admin"
        }

        if(authResponseCode != 200){
            fakeKeystoneV3Service.validateTokenHandler = {
                tokenId, request ->
                    new Response(authResponseCode, null, null, responseBody)
            }
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqProject/",
                method: 'GET',
                headers: ['content-type': 'application/json',
                          'X-Subject-Token': fakeKeystoneV3Service.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-authorization") == "Proxy"
        mc.handlings[0].request.headers.getFirstValue("X-Identity-Status") == "Indeterminate"

        where:
        reqProject  | authResponseCode | responseCode   |responseBody
        "p500"      | 401              | "200"          |"Unauthorized"
        "p502"      | 404              | "200"          |fakeKeystoneV3Service.identityFailureJsonRespTemplate
    }
}
