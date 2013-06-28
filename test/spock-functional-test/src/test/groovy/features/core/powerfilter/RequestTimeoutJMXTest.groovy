package features.core.powerfilter

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response
import spock.lang.Ignore

class RequestTimeoutJMXTest extends ReposeValveTest {

    String PREFIX = "\"repose-node1-com.rackspace.papi\":type=\"RequestTimeout\",scope=\""

    String NAME_OPENREPOSE_ENDPOINT = "\",name=\"localhost:10001/root_path\""
    String ALL_ENDPOINTS = "\",name=\"All Endpoints\""
    String TIMEOUT_TO_ORIGIN = PREFIX + "TimeoutToOrigin" + NAME_OPENREPOSE_ENDPOINT

    String ALL_TIMEOUT_TO_ORIGIN = PREFIX + "TimeoutToOrigin" + ALL_ENDPOINTS

    def handlerTimeout = { request -> return new Response(408, 'WIZARD FAIL') }

    def setup() {
        repose.applyConfigs("features/core/powerfilter/common")
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()
        repose.stop()
    }

    def "when responses have timed out, should increment RequestTimeout mbeans for specific endpoint"() {

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout])
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout])

        then:
        repose.jmx.getMBeanAttribute(TIMEOUT_TO_ORIGIN, "Count") == 2
    }


    def "when responses have timed out, should increment RequestTimeout mbeans for all endpoint"() {

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout])
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout])

        then:
        repose.jmx.getMBeanAttribute(ALL_TIMEOUT_TO_ORIGIN, "Count") == 2
    }

    def "when SOME responses have timed out, should increment RequestTimeout mbeans for specific endpoint only for timeouts"() {

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout])
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout])
        deproxy.makeRequest(reposeEndpoint + "/endpoint")
        deproxy.makeRequest(reposeEndpoint + "/endpoint")

        then:
        repose.jmx.getMBeanAttribute(ALL_TIMEOUT_TO_ORIGIN, "Count") == 2
    }
}
