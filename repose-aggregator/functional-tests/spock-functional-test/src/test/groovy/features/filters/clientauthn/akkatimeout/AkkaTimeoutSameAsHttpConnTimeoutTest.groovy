package features.filters.clientauthn.akkatimeout

import framework.ReposeValveTest
import framework.category.Slow
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
/**
 * Created by jennyvo on 1/5/15.
 */
@Category(Slow)
class AkkaTimeoutSameAsHttpConnTimeoutTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/akkatimeout", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)


    }

    def cleanupSpec() {
        if(deproxy)
            deproxy.shutdown()
        if(repose)
            repose.stop()
    }

    def setup(){
        fakeIdentityService.resetHandlers()
    }

    def "akka timeout test, auth response time out less than http connection time out" () {
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = 613
            service_admin_role = "not-admin"
            client_userid = 1234
            sleeptime = 29000
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/613/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then: "Request should not be passed from repose"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }

    def "akka timeout test, auth response time out greater than http connection time out" () {
        reposeLogSearch.cleanLog()
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = 613
            service_admin_role = "not-admin"
            client_userid = 1234
            sleeptime = 35000
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/613/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then: "Request should not be passed from repose"
        mc.receivedResponse.code == "500"
        mc.handlings.size() == 0
        reposeLogSearch.searchByString("java.util.concurrent.TimeoutException: Futures timed out after .31000 milliseconds.").size() > 0
    }
}
