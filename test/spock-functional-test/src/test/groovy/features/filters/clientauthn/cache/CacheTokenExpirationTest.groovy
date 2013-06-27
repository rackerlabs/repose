package features.filters.clientauthn.cache

import features.filters.clientauthn.IdentityServiceResponseSimulator
import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain

class CacheTokenExpirationTest extends ReposeValveTest {

    def originEndpoint
    def identityEndpoint

    IdentityServiceResponseSimulator fakeIdentityService

    def setup() {
        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthn/common")
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.getProperty("target.port").toInteger(),'origin service')


    }

    def cleanup() {
        if (deproxy) {
            deproxy.shutdown()
        }
        repose.stop()
    }

    // D-13310 Repose should cache tokens with TTL's longer than MAX_INT for a duration of MAX_INT
    def "When Identity responds with a TTL > MAX_INT, Repose should cache for a duration of MAX_INT"() {

        given:
        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService = new IdentityServiceResponseSimulator()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.ttlDurationInDays = 40
        identityEndpoint = deproxy.addEndpoint(properties.getProperty("identity.port").toInteger(),
                'identity service', null, fakeIdentityService.handler)

        when: "I send a GET request to REPOSE with an X-Auth-Token header"
        fakeIdentityService.validateTokenCount = 0
        MessageChain mc = deproxy.makeRequest(reposeEndpoint, 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "REPOSE should validate the token and then pass the request to the origin service"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        fakeIdentityService.validateTokenCount == 1

        when: "I send a GET request to REPOSE with the same X-Auth-Token header"
        fakeIdentityService.validateTokenCount = 0
        mc = deproxy.makeRequest(reposeEndpoint, 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Repose should use the cache, not call out to the fake identity service, and pass the request to origin service"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        fakeIdentityService.validateTokenCount == 0

        when: "I troubleshoot the REPOSE logs"
        def foundLogs = reposeLogSearch.searchByString("Token TTL \\(" + clientToken + "\\) exceeds max expiration, setting to default max expiration")

        then: "I should have a WARN log message"
        foundLogs.size() == 1
        foundLogs[0].contains("WARN")
    }

}
