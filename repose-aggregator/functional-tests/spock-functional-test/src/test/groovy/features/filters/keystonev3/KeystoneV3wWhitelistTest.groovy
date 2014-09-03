package features.filters.keystonev3

import framework.ReposeValveTest
import framework.mocks.MockKeystoneV3Service
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 8/27/14.
 * Test keystone v3 filter with whitelist config
 */
class KeystoneV3wWhitelistTest extends ReposeValveTest{
    def static originEndpoint
    def static identityEndpoint
    def static MockKeystoneV3Service fakeKeystoneV3Service

    def setupSpec() {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev3",params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev3/whitelist",params)
        repose.start()
        waitUntilReadyToServiceRequests('401')

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeKeystoneV3Service = new MockKeystoneV3Service(properties.identityPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null,fakeKeystoneV3Service.handler)
    }

    def cleanupSpec() {
        if(deproxy)
            deproxy.shutdown()
        if(repose)
            repose.stop()
    }

    @Unroll ("#uriPattern expect #responseCode")
    def "Test request with uri in whitelist pattern req should pass without authenticate" (){
        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/$uriPattern",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        //mc.handlings.size() == 1
        mc.orphanedHandlings.size() == 0

        where:
        uriPattern          | responseCode
        "public-info"       | "200"
        "test-info"         | "200"
        ""                  | "401"
        "servers-info"      | "401"
    }

    def "Test send request with user token" () {
        given:
        def reqDomain = fakeKeystoneV3Service.client_domainid
        def reqUserId = fakeKeystoneV3Service.client_userid

        fakeKeystoneV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_domainid = reqDomain
            client_userid = reqUserId
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqDomain",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Subject-Token': fakeKeystoneV3Service.client_token,
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        //mc.orphanedHandlings.size() == 0
    }
}
