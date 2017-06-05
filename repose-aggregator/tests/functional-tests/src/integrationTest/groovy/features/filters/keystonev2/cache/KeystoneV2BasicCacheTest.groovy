package features.filters.keystonev2.cache

import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class KeystoneV2BasicCacheTest extends ReposeValveTest {

    static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)

        deproxy.addEndpoint(params.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(params.identityPort, params.targetPort)
        deproxy.addEndpoint(port: params.identityPort, name: 'identity service', defaultHandler: fakeIdentityV2Service.handler)

        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def setup() {
        fakeIdentityV2Service.resetDefaultParameters()
    }

    def "The X-Auth-Token-Key header is sent whether or not the token is cached"() {
        when: "the token is retrieved from Keystone v2"
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint + "/servers/test",
            headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "the X-Auth-Token-Key request header is sent"
        fakeIdentityV2Service.validateTokenCount == 1
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-Auth-Token-Key") == "IDENTITY:V2:TOKEN:${fakeIdentityV2Service.client_token}"

        when: "the token is retrieved from the cache"
        mc = deproxy.makeRequest(
            url: reposeEndpoint + "/servers/test",
            headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "the X-Auth-Token-Key request header is sent"
        fakeIdentityV2Service.validateTokenCount == 1
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-Auth-Token-Key") == "IDENTITY:V2:TOKEN:${fakeIdentityV2Service.client_token}"
    }
}
