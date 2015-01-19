package features.filters.clientauthn.akkatimeout
import framework.ReposeValveTest
import framework.category.Slow
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

import javax.servlet.http.HttpServletResponse
/**
 * Created by jennyvo on 1/5/15.
 *  Previously akkatimeout was hard code to 50 second now set the same as http connection
 *  timeout. Test is checking If the HttpClient connection timeout is greater than 50 seconds,
 *  then it is not triggered prematurely at 50 seconds.
 */
@Category(Slow)
class HttpConnTimeoutGreaterThan50SecTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/akkatimeout", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/akkatimeout/httpConnTimeout60sec", params)
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

    def "akka timeout test, Http conn timeout is greater than 50 seconds, then it is not triggered prematurely at 50 seconds " () {
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = 613
            service_admin_role = "not-admin"
            client_userid = 1234
            sleeptime = 55000
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
            client_tenant = 614
            service_admin_role = "not-admin"
            client_userid = 12345
            sleeptime = 62000
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
        mc.receivedResponse.code == HttpServletResponse.SC_GATEWAY_TIMEOUT.toString()
        mc.handlings.size() == 0
        sleep(1000)
        reposeLogSearch.searchByString("Error acquiring value from akka .GET. or the cache. Reason: Futures timed out after .61000 milliseconds.").size() > 0
        reposeLogSearch.searchByString("NullPointerException").size() == 0
    }

    def "akka timeout POST test, auth response time out greater than http connection time out" () {
        reposeLogSearch.cleanLog()
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = 613
            service_admin_role = "not-admin"
            client_userid = 1234
            sleeptime = 62000
            fakeIdentityService.generateTokenHandler = {
                request, xml ->
                    new Response(500, null, null, "")
            }
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/613/",
                method: 'POST',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then: "Request should not be passed from repose"
        mc.receivedResponse.code == HttpServletResponse.SC_GATEWAY_TIMEOUT.toString()
        mc.handlings.size() == 0
        sleep(1000)
        reposeLogSearch.searchByString("Error acquiring value from akka .POST. or the cache. Reason: Futures timed out after .61000 milliseconds.").size() > 0
        reposeLogSearch.searchByString("NullPointerException").size() == 0
    }
}
