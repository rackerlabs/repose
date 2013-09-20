package features.core.powerfilter

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Response

@Category(Slow.class)
class RequestTimeoutJMXTest extends ReposeValveTest {

    String PREFIX = "\"repose-node1-com.rackspace.papi\":type=\"RequestTimeout\",scope=\""

    String NAME_OPENREPOSE_ENDPOINT = "\",name=\"localhost:10001/root_path\""
    String ALL_ENDPOINTS = "\",name=\"All Endpoints\""

    String TIMEOUT_TO_ORIGIN = PREFIX + "TimeoutToOrigin" + NAME_OPENREPOSE_ENDPOINT
    String ALL_TIMEOUT_TO_ORIGIN = PREFIX + "TimeoutToOrigin" + ALL_ENDPOINTS

    def handlerTimeout = { request -> return new Response(408, 'WIZARD FAIL') }

    def setupSpec() {
        repose.applyConfigs("features/core/powerfilter/common")
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()
        repose.stop()
    }

    def "when responses have timed out, should increment RequestTimeout mbeans for specific endpoint"() {
        given:
        def target = repose.jmx.getMBeanAttribute(TIMEOUT_TO_ORIGIN, "Count")
        target = (target == null) ? 0 : target

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout])
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout])

        then:
        repose.jmx.getMBeanAttribute(TIMEOUT_TO_ORIGIN, "Count") == (target + 2)
    }


    def "when responses have timed out, should increment RequestTimeout mbeans for all endpoint"() {
        given:
        def target = repose.jmx.getMBeanAttribute(ALL_TIMEOUT_TO_ORIGIN, "Count")
        target = (target == null) ? 0 : target

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout])
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout])

        then:
        repose.jmx.getMBeanAttribute(ALL_TIMEOUT_TO_ORIGIN, "Count") == (target + 2)
    }

    def "when SOME responses have timed out, should increment RequestTimeout mbeans for specific endpoint only for timeouts"() {
        given:
        def target = repose.jmx.getMBeanAttribute(ALL_TIMEOUT_TO_ORIGIN, "Count")
        target = (target == null) ? 0 : target

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout])
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout])
        deproxy.makeRequest(reposeEndpoint + "/endpoint")
        deproxy.makeRequest(reposeEndpoint + "/endpoint")

        then:
        repose.jmx.getMBeanAttribute(ALL_TIMEOUT_TO_ORIGIN, "Count") == (target + 2)
    }
}
