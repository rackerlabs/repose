package features.core.powerfilter

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Ignore

@Category(Slow.class)
class ResponseCodeJMXTest extends ReposeValveTest {

    String PREFIX = "\"repose-node1-com.rackspace.papi\":type=\"ResponseCode\",scope=\""

    String NAME_2XX = "\",name=\"2XX\""
    String ALL_2XX = PREFIX + "All Endpoints" + NAME_2XX
    String REPOSE_2XX = PREFIX + "Repose" + NAME_2XX

    String NAME_5XX = "\",name=\"5XX\""
    String ALL_5XX = PREFIX + "All Endpoints" + NAME_5XX
    String REPOSE_5XX = PREFIX + "Repose" + NAME_5XX

    def handler5XX = { request -> return new Response(502, 'WIZARD FAIL') }

    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/common", params)
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()
        repose.stop()
    }

    // Greg/Dimitry: Is it expected that all2XX and repose2XX are equal?  It's not the sum of repose responses + origin service
    // responses?
    def "when sending requests, response code counters should be incremented"() {
        given:
        // the initial values are equivalent the the number of calls made in the when block
        def repose2XXtarget = repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count")
        repose2XXtarget = (repose2XXtarget == null) ? 3 : repose2XXtarget + 3
        def all2XXtarget = repose.jmx.getMBeanAttribute(ALL_2XX, "Count")
        all2XXtarget = (all2XXtarget == null) ? 3 : all2XXtarget + 3
        def repose5XXtarget = repose.jmx.getMBeanAttribute(REPOSE_5XX, "Count")
        repose5XXtarget = (repose5XXtarget == null) ? 0 : repose5XXtarget
        def all5XXtarget = repose.jmx.getMBeanAttribute(ALL_5XX, "Count")
        all5XXtarget = (all5XXtarget == null) ? 0 : all5XXtarget
        def responses = []

        when:
        responses.add(deproxy.makeRequest(url:reposeEndpoint + "/endpoint"))
        responses.add(deproxy.makeRequest(url:reposeEndpoint + "/endpoint"))
        responses.add(deproxy.makeRequest(url:reposeEndpoint + "/cluster"))

        then:
        repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count") == repose2XXtarget
        repose.jmx.getMBeanAttribute(ALL_2XX, "Count") == all2XXtarget
        repose.jmx.getMBeanAttribute(REPOSE_5XX, "Count").is(null)
        repose.jmx.getMBeanAttribute(ALL_5XX, "Count").is(null)

        responses.each { MessageChain mc ->
            assert(mc.receivedResponse.code == "200")
        }
    }

    def "when responses have 2XX and 5XX status codes, should increment 2XX and 5XX mbeans"() {
        given:
        def repose2XXtarget = repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count")
        repose2XXtarget = (repose2XXtarget == null) ? 1 : repose2XXtarget + 1
        def all2XXtarget = repose.jmx.getMBeanAttribute(ALL_2XX, "Count")
        all2XXtarget = (all2XXtarget == null) ? 1 : all2XXtarget + 1
        def repose5XXtarget = repose.jmx.getMBeanAttribute(REPOSE_5XX, "Count")
        repose5XXtarget = (repose5XXtarget == null) ? 1 : repose5XXtarget + 1
        def all5XXtarget = repose.jmx.getMBeanAttribute(ALL_5XX, "Count")
        all5XXtarget = (all5XXtarget == null) ? 1 : all5XXtarget + 1

        when:
        MessageChain mc1 = deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handler5XX])
        MessageChain mc2 = deproxy.makeRequest(url:reposeEndpoint + "/cluster")

        then:
        mc1.receivedResponse.code == "502"
        mc2.receivedResponse.code == "200"
        repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count") == repose2XXtarget
        repose.jmx.getMBeanAttribute(ALL_2XX, "Count") == all2XXtarget
        repose.jmx.getMBeanAttribute(REPOSE_5XX, "Count") == repose5XXtarget
        repose.jmx.getMBeanAttribute(ALL_5XX, "Count") == all5XXtarget
    }

    /**
     *  TODO:
     *
     *  1) Need to verify counts for:
     *     - endpoint
     *     - cluster
     *     - All Endpoints
     *  2) Need to verify that Repose 5XX is sum of all non "All Endpoints" 5XX bean
     *  3) Need to verify that Repose 2XX is sum of all non "All Endpoints" 2XX bean
     */
    @Ignore
    def "when sending requests to service cluster, response codes should be recorded"() {

        when:

        // NOTE:  We verify that Repose is up and running by sending a GET request in repose.start()
        // This is logged as well, so we need to add this to our count

        deproxy.makeRequest(url:reposeEndpoint + "/endpoint");
        deproxy.makeRequest(url:reposeEndpoint + "/endpoint");
        deproxy.makeRequest(url:reposeEndpoint + "/cluster");

        def reposeCount = repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count")

        then:
        reposeCount == 4
    }
}
