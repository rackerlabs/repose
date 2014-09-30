package features.filters.identitybasicauth
import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.apache.commons.codec.binary.Base64
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.HttpHeaders
/**
 * Created by jennyvo on 9/24/14.
 * simple token cache timeout test
 */
class BasicAuthCacheTimeoutTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityService fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/identitybasicauth", params);
        repose.configurationProvider.applyConfigs("features/filters/identitybasicauth/cache", params);
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
        fakeIdentityService.checkTokenValid = true
    }

    def setup() {
        fakeIdentityService.with {
            // This is required to ensure that one piece of the authentication data is changed
            // so that the cached version in the Akka Client is not used.
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
        }
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def "Ensure that subsequent calls within the cache timeout are retrieving the token from the cache"() {
        given: "the HTTP Basic authentication header containing the User Name and API Key"
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "multiple requests that have the same HTTP Basic authentication header"
        MessageChain mc0 = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)
        MessageChain mc1 = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "get the token from the cache"
        mc0.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc0.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        mc0.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
        mc1.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc1.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        mc1.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
        mc1.orphanedHandlings.size() == 0
    }

    def "Ensure that subsequent calls outside the cache timeout are retrieving a new token not from the cache"() {
        given: "the HTTP Basic authentication header containing the User Name and API Key"
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "multiple requests that have the same HTTP Basic authentication header, but are separated by more than the cache timeout"
        MessageChain mc0 = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)
        sleep 3000 // How do I get this programmatically from the config.
        MessageChain mc1 = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "get the token from the Identity (Keystone) service"
        mc0.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc0.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        mc0.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
        mc1.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc1.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        mc1.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
        mc1.orphanedHandlings.size() == 1
    }
}
