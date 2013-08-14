package features.services.httpconnectionpool

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain


class HttpClientServiceTest extends ReposeValveTest {

    def setup(){
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanup(){
        if (deproxy)
            deproxy.shutdown()
    }

    def "repose should use http conn pool service for origin service" () {
        given:
        repose.applyConfigs("features/services/httpconnectionpool/common",
                "features/services/httpconnectionpool/withconfig")
        repose.start()
        waitUntilReadyToServiceRequests()

        when:
        MessageChain mc = deproxy.makeRequest([url:reposeEndpoint + "/cluster",headers:['x-trace-request': 'true']])

        then:
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        // TODO: how to verify it is using the conn pool????
    }

    def "repose completes the handling of inflight connections when reconfiguring CP service"() {}

    def "shutting down repose should release all connections"() {}

    def "repose should use http conn pool service for Client Auth-N" () {}

    def "repose should use http conn pool service for DD service" () {}

}