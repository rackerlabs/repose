package features.filters.keystonev3

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import framework.mocks.MockKeystoneV3Service
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Ignore

/**
 * Created by jennyvo on 9/4/14.
 * test token max expired
 */
@Ignore ("Ignore this test for now since we haven't explicitly logged the WARN message to client")
class KeystoneV3CacheTokenExpirationTest extends ReposeValveTest{
    def originEndpoint
    def identityEndpoint

    MockKeystoneV3Service fakeKeystoneV3Service

    def setup () {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("/features/filters/keystonev3", params)
        repose.configurationProvider.applyConfigs("/features/filters/keystonev3/common", params)
        repose.configurationProvider.applyConfigs("/features/filters/keystonev3/connectionpooling", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort,'origin service')
    }

    def cleanupSpec() {
        if(deproxy)
            deproxy.shutdown()
        if(repose)
            repose.stop()
    }

    def "When Identity responds with a TTL > MAX_INT, Repose should cache for a duration of MAX_INT"() {

        given:
        def clientToken = UUID.randomUUID().toString()
        fakeKeystoneV3Service = new MockKeystoneV3Service(properties.identityPort, properties.targetPort)
        fakeKeystoneV3Service.client_token = clientToken
        fakeKeystoneV3Service.tokenExpiresAt = (new DateTime()).plusDays(40);

        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeKeystoneV3Service.handler)

        when: "I send a GET request to REPOSE with an X-Subject-Token header"
        fakeKeystoneV3Service.resetCounts()
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Subject-Token': fakeKeystoneV3Service.client_token])

        then: "REPOSE should validate the token and then pass the request to the origin service"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        fakeKeystoneV3Service.validateTokenCount == 1

        when: "I send a GET request to REPOSE with the same X-Auth-Token header"
        fakeKeystoneV3Service.resetCounts()
        mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Subject-Token': fakeKeystoneV3Service.client_token])

        then: "Repose should use the cache, not call out to the fake identity service, and pass the request to origin service"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        fakeKeystoneV3Service.validateTokenCount == 0

        when: "I troubleshoot the REPOSE logs"
        def foundLogs = reposeLogSearch.searchByString("Token TTL \\(" + clientToken + "\\) exceeds max expiration, setting to default max expiration")

        then: "I should have a WARN log message"
        foundLogs.size() == 1
        foundLogs[0].contains("WARN")
    }
}
