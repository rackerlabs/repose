package features.filters.identityv3

import framework.ReposeValveTest
import framework.mocks.MockIdentityV3Service
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain


class IdentityV3Impersonation extends ReposeValveTest{
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/common",params)
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

    def setup() {
        fakeIdentityV3Service.resetParameters()
    }

    def "when requesting with impersonation, repose should add X-Impersonator-Name and X-Impersonator-Id" () {
        given:
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_domainid = 12345
            client_userid = 123456
            impersonate_name="impersonator_name"
            impersonate_id="567"
        }

        when: "User passes a request with impersonation through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/12345/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token,
                ]
        )

        then: "repose should add X-Impersonator-Name and X-Impersonator-Id"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-Impersonator-Name") == fakeIdentityV3Service.impersonate_name
        mc.handlings[0].request.headers.getFirstValue("X-Impersonator-Id") == fakeIdentityV3Service.impersonate_id
    }

    def "when requesting without impersonation, repose should not add any impersonator headers" () {
        given:
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_domainid = 789
            client_userid = 8910
        }

        when: "User passes a request through repose with no impersonation"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/789/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token,
                ]
        )

        then: "repose should not add any impersonator headers"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        !mc.handlings[0].request.headers.contains("X-Impersonator-Name")
        !mc.handlings[0].request.headers.contains("X-Impersonator-Id")
    }
}
