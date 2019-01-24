package features.services.httpconnectionpool

import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handlers
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request

class HttpClientServiceProxyTest extends ReposeValveTest {

    static int originServiceEndpointCount
    static int proxyEndpointCount

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(
            port: properties.targetPort,
            defaultHandler: { Request request ->
                originServiceEndpointCount++
                Handlers.simpleHandler(request)
            }
        )
        deproxy.addEndpoint(
            port: properties.targetPort2,
            defaultHandler: { Request request ->
                proxyEndpointCount++
                Handlers.simpleHandler(request)
            }
        )

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/proxy", params)
        repose.start()
    }

    def setup() {
        originServiceEndpointCount = 0
        proxyEndpointCount = 0
    }

    def "the HTTP Client Service should support proxies"() {
        given: "the default connection pool is configured with a proxy"

        when: "a request is made to Repose"
        MessageChain messageChain = deproxy.makeRequest(reposeEndpoint)

        then: "the request will be sent to the proxy"
        messageChain.handlings.size() == 1
        proxyEndpointCount == 1
        originServiceEndpointCount == 0
    }
}
