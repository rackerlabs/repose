package features.services.httpconnectionpool

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain


class HttpClientServiceTest extends ReposeValveTest {

    def setup(){
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def cleanup(){
        if (deproxy)
            deproxy.shutdown()
    }

    def "repose should use http conn pool service for origin service" () {
        given:
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/withconfig", params)
        repose.start()
        waitUntilReadyToServiceRequests()

        when:
        MessageChain mc = deproxy.makeRequest([url:reposeEndpoint + "/cluster",headers:['x-trace-request': 'true']])

        then:
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
    }
}
