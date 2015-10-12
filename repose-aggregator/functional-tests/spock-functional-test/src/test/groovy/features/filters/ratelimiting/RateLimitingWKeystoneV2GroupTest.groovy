package features.filters.ratelimiting

import framework.ReposeValveTest
import framework.mocks.MockIdentityV2Service
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

/**
 * Created by jennyvo on 10/12/15.
 *  This test is similar to RateLimitingWClientAuthGroupTest except using KeystoneV2 as auth filter
 */
class RateLimitingWKeystoneV2GroupTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityV2Service fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/withkeystonev2groups", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    def setup() {
        fakeIdentityService.resetHandlers()
    }

    def "Rate Limit on keystone v2 group"() {
        given:
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            service_admin_role = "not-admin"
        }

        when: "when request pass through repose hasn't hit ratelimit"
        MessageChain mc = deproxy.makeRequest(
                url: reposeEndpoint + "/servers/123456",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("x-pp-groups")

        when: "when request pass through repose hasn't hit ratelimit"
        mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/123456",
                method: 'PUT',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("x-pp-groups")

        when: "when request pass through repose hit ratelimit"
        mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/123456",
                method: 'PUT',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then:
        mc.receivedResponse.code == "413"
    }
}