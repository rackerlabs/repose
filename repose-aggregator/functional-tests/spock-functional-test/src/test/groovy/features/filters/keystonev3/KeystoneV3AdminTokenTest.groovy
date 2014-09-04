package features.filters.keystonev3

import framework.ReposeValveTest
import framework.mocks.MockKeystoneV3Service
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by jennyvo on 9/4/14.
 */
class KeystoneV3AdminTokenTest extends ReposeValveTest{
    def static originEndpoint
    def static identityEndpoint
    def static MockKeystoneV3Service fakeKeystoneV3Service

    def setupSpec() {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev3/common",params)
        repose.start()
        waitUntilReadyToServiceRequests('401')

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeKeystoneV3Service = new MockKeystoneV3Service(properties.identityPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeKeystoneV3Service.handler)
    }

    def cleanupSpec() {
        if(deproxy)
            deproxy.shutdown()
        if(repose)
            repose.stop()
    }

    def setup(){
        sleep 500
        fakeKeystoneV3Service.resetHandlers()
    }

    @Unroll("Sending request with admin response set to HTTP #adminRespCode receive #respCode")
    def "when failing to authenticate admin client"() {
        given:
        fakeKeystoneV3Service.with {
            client_domainid = reqDomain
            client_userid = reqDomain
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            generateTokenHandler = {
                request ->
                    new Response(adminRespCode, null, null, responseBody)
            }
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqDomain/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Subject-Token': fakeKeystoneV3Service.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == respCode
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == orphanedHandlings

        where:
        reqDomain | adminRespCode | respCode | responseBody                                              | orphanedHandlings
        1111      | 500           | "500"    | ""                                                        | 1
        1112      | 404           | "500"    | fakeKeystoneV3Service.identityFailureAuthJsonRespTemplate | 1
    }
}
