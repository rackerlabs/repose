package features.filters.keystonev3

import framework.ReposeValveTest
import framework.mocks.MockKeystoneV3Service
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

/**
 * Created by jennyvo on 8/25/14.
 */
class KeystoneV3Test extends ReposeValveTest{
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
                url: "$reposeEndpoint/servers/$reqDomain/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Subject-Token': fakeKeystoneV3Service.client_token,
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == 200
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == 0
    }
}
