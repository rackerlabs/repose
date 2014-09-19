package features.filters.identitybasicauth

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.apache.commons.codec.binary.Base64
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.HttpHeaders

import static framework.TestUtils.timedSearch

/**
 * Created by jennyvo on 9/17/14.
 * Basic Auth filter can't be used alone, have to use with client-auth filter
 */
class BasicAuthStandaloneTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    //Start repose once for this particular translation test
    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/identitybasicauth", params);
        repose.configurationProvider.applyConfigs("features/filters/identitybasicauth/onlybasicauth", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    def "when start repose with basic auth in filter without client-auth" () {
        when: "send request without credential"
        MessageChain mc = deproxy.makeRequest([url: reposeEndpoint])

        then: "request should pass as no basic auth filter"
        mc.receivedResponse.code == HttpServletResponse.SC_UNAUTHORIZED.toString()
        mc.handlings.size() == 1
        !mc.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    def "when start repose with x-auth-token, basicauth shouldn't work" () {
        given:
        def headers = [
                "X-Auth-Token": fakeIdentityService.client_token
        ]

        when: "send request without credential"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers)

        then: "request should pass as no basic auth filter"
        mc.receivedResponse.code == HttpServletResponse.SC_UNAUTHORIZED.toString()
        mc.handlings.size() == 0
        mc.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    def "when send request with credential" () {
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "the request does have an HTTP Basic authentication header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "request should pass as no basic auth filter"
        mc.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("Authorization")
        !mc.handlings[0].request.headers.getFirstValue("WWW-Authenticate")
        !mc.handlings[0].request.headers.contains("X-Auth-Token")
        mc.getOrphanedHandlings().empty
        timedSearch(10) {
            reposeLogSearch.searchByString("WARNING").size()  > 0
        }
    }
}
