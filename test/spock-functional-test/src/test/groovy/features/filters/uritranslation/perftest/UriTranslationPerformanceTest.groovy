package features.filters.uritranslation.perftest

import framework.ReposeValveTest
import org.joda.time.DateTime
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response

class UriTranslationPerformanceTest extends ReposeValveTest {


    def "performance test to ensure header translation is on par or better than translation filter"() {
        given:
        int totalRequests = 1000

        when: "I make 1000 requests through the uri stripper filter"
        repose.applyConfigs("features/filters/uristripper/common", "features/filters/uristripper/locationRewrite")
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
        def averageWithHdrXlate = makeRequests(totalRequests)
        repose.stop()
        deproxy.shutdown()

        and: "I make 1000 requests through translation filter"
        repose.applyConfigs("features/filters/translation/common", "features/filters/uritranslation/perftest")
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
        def averageWithTranslationFilter = makeRequests(totalRequests)
        repose.stop()
        deproxy.shutdown()

        then: "Average response time for uri stripper is <= avg response time for translation filter"
        println("URI Stripper: " + averageWithHdrXlate + " Translation: " + averageWithTranslationFilter)
        averageWithHdrXlate <= averageWithTranslationFilter * 1.1
    }


    def makeRequests(int totalRequests) {
        long totalMillis = 0
        def resp = { request -> return new Response(301, "Moved Permanently") }

        // warm up, ignore response times
        for (int i : 100) {
            deproxy.makeRequest([url: reposeEndpoint + "/v1/12345/path/to/resource", defaultHandler: resp])
        }

        // now let's capture response times
        for (int i: 1..totalRequests) {
            // start time
            def timeStart = new DateTime()
            MessageChain mc = deproxy.makeRequest([url: reposeEndpoint + "/v1/12345/path/to/resource", defaultHandler: resp])
            def timeStop = new DateTime()
            def elapsedMillis = timeStop.millis - timeStart.millis
            totalMillis += elapsedMillis
        }

        return totalMillis / totalRequests
    }

}
