package features.filters.valkyrie

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import framework.mocks.MockValkyrie
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

/**
 * Created by jennyvo on 4/21/15.
 */
class ValkyrieAuthorizationCacheTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static valkyrieEndpoint

    def static MockIdentityService fakeIdentityService
    def static MockValkyrie fakeValkyrie
    def static Map params = [:]

    def static random = new Random()

    def setupSpec() {
        deproxy = new Deproxy()

        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
        fakeIdentityService.checkTokenValid = true

        fakeValkyrie = new MockValkyrie(properties.valkyriePort)
        valkyrieEndpoint = deproxy.addEndpoint(properties.valkyriePort, 'valkyrie service', null, fakeValkyrie.handler)
    }

    def setup() {
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }
    def "Test Valkyrie Authorization Cache"() {
        given: "A device ID with a particular permission level defined in Valykrie"
        def tenantID = randomTenant()
        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
            client_tenant = tenantID
        }

        fakeValkyrie.with {
            device_id = "520707"
            device_perm = "view_product"
        }

        when: "a request is made against a device with Valkyrie set permissions"
        fakeValkyrie.resetCounts()
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/520707", method: "GET",
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                ]
        )

        then: "check response"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        fakeValkyrie.getAuthorizationCount() == 1

        when: "send another request with same device, permission same client_token"
        fakeValkyrie.resetCounts()
        mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/520707", method: "GET",
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                ]
        )

        then: "check response"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        fakeValkyrie.getAuthorizationCount() == 0
    }

    def "Test Cache Timeout" () {
        given: "A device ID with a particular permission level defined in Valykrie"
        DateTime initialCacheValidation = DateTime.now()
        def tenantID = randomTenant()
        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
            client_tenant = tenantID
        }

        fakeValkyrie.with {
            device_id = "520708"
            device_perm = "admin_product"
        }

        when: "subsequence request with same device, permission same client_token not exceeding the cache expiration"
        fakeValkyrie.resetCounts()
        DateTime minimumCacheExpiration = initialCacheValidation.plusMillis(3000)
        while (minimumCacheExpiration.isAfterNow()) {
            MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/520708", method: "GET",
                    headers: [
                            'content-type': 'application/json',
                            'X-Auth-Token': fakeIdentityService.client_token,
                    ]
            )
            mc.receivedResponse.code.equals('200')
        }

        then: "should count for 1st time then all sub-sequence calls should hit cache"
        fakeValkyrie.getAuthorizationCount() == 1

        when: "Cache is expire"
        fakeValkyrie.resetCounts()
        sleep(500)

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/520708", method: "GET",
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                ]
        )

        then: "should re-authenticate"
        fakeValkyrie.getAuthorizationCount() == 1

    }
    def String randomTenant() {
        "hybrid:" + random.nextInt()
    }
}
