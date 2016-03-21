package features.filters.headertranslation

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 3/21/16.
 */
class HeaderTranslationWQualityTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/headertranslation/common", params)
        repose.configurationProvider.applyConfigs("features/filters/headertranslation/wquality", params)
        repose.start()
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }
    @Unroll("Request Verb: #method Headers: #reqHeaders")
    def "when translating CSL request headers"() {

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest(url: (String) reposeEndpoint, method: method, headers: reqHeaders)
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "origin receives translated headers"
        sentRequest.request.getHeaders().contains("x-rax-username")
        sentRequest.request.getHeaders().getFirstValue("x-rax-username") == "a;q=0.5"
        sentRequest.request.getHeaders().contains("x-rax-tenants")
        sentRequest.request.getHeaders().getFirstValue("x-rax-tenants") == "b"
        sentRequest.request.getHeaders().contains("x-rax-roles")
        sentRequest.request.getHeaders().getFirstValue("x-rax-roles") == "c;q=0.2"
        sentRequest.request.getHeaders().contains("x-pp-user")
        sentRequest.request.getHeaders().contains("x-tenant-name")
        sentRequest.request.getHeaders().contains("x-roles")

        where:
        method | reqHeaders
        "POST" | ["x-pp-user": "a", "x-tenant-name": "b", "x-roles": "c"]
        "GET"  | ["x-pp-user": "a", "x-tenant-name": "b", "x-roles": "c"]
    }
}
