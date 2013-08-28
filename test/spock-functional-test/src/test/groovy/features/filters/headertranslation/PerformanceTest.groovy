package features.filters.headertranslation

import framework.ReposeValveTest
import groovy.time.TimeCategory
import groovy.time.TimeDuration
import org.joda.time.DateTime
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain


class PerformanceTest extends ReposeValveTest {


    def "performance test configs produce expected responses"() {
        when: "I make a request through header translation filter"
        repose.applyConfigs( "features/filters/headertranslation/common",
                "features/filters/headertranslation/oneToMany" )
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
        MessageChain mcHdrXlate = deproxy.makeRequest(reposeEndpoint, "GET", ["X-Header-A":"12345", "X-Header-B":"abcde"])

        repose.stop()
        deproxy.shutdown()

        and: "I make a request through header translation filter"
        repose.applyConfigs( "features/filters/headertranslation/common",
                "features/filters/headertranslation/oneToMany" )
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
        MessageChain mcTranslation = deproxy.makeRequest(reposeEndpoint, "GET", ["X-Header-A":"12345", "X-Header-B":"abcde"])

        repose.stop()
        deproxy.shutdown()

        then:

        def mcHdrHandling = mcHdrXlate.handlings.get(0)

        mcHdrHandling.request.headers.contains("X-Header-A")
        mcHdrHandling.request.headers.contains("X-Header-C")
        mcHdrHandling.request.headers.contains("X-Header-D")
        mcHdrHandling.request.headers.getFirstValue("X-Header-C") == mcHdrXlate.sentRequest.headers.getFirstValue("X-Header-A")
        mcHdrHandling.request.headers.getFirstValue("X-Header-D") == mcHdrXlate.sentRequest.headers.getFirstValue("X-Header-A")

        then:
        def mcTransHandling = mcTranslation.handlings.get(0)
        mcTransHandling.request.headers.contains("X-Header-A")
        mcTransHandling.request.headers.contains("X-Header-C")
        mcTransHandling.request.headers.contains("X-Header-D")
        mcTransHandling.request.headers.getFirstValue("X-Header-C") == mcTranslation.sentRequest.headers.getFirstValue("X-Header-A")
        mcTransHandling.request.headers.getFirstValue("X-Header-D") == mcTranslation.sentRequest.headers.getFirstValue("X-Header-A")
    }

    def "performance test to ensure header translation is on par or better than translation filter"() {
        given:
        int totalRequests = 1000

        when: "I make 100 requests through header translation filter"
        repose.applyConfigs( "features/filters/headertranslation/common",
                "features/filters/headertranslation/oneToMany" )
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
        def averageWithHdrXlate = makeRequests(totalRequests)
        repose.stop()
        deproxy.shutdown()

        and: "I make 100 requests through translation filter"
        repose.applyConfigs( "features/filters/headertranslation/perftest")
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
        def averageWithTranslationFilter = makeRequests(totalRequests)
        repose.stop()
        deproxy.shutdown()

        then: "Average response time for header translation is <= avg response time for translation filter"
        println("HDR XLATE: " + averageWithHdrXlate + " Translation: " + averageWithTranslationFilter)
        averageWithHdrXlate <= averageWithTranslationFilter * 1.1
    }


    def makeRequests(int totalRequests) {
        long totalMillis = 0

        // warm up, ignore response times
        for (int i : 1..100) {
            deproxy.makeRequest(reposeEndpoint, "GET", ["X-Header-A":"12345", "X-Header-B":"abcde"])
        }

        // now let's capture response times
        for (int i : 1..totalRequests) {
            // start time
            def timeStart = new DateTime()
            MessageChain mc = deproxy.makeRequest(reposeEndpoint, "GET", ["X-Header-A":"12345", "X-Header-B":"abcde"])
            def timeStop = new DateTime()
            def elapsedMillis = timeStop.millis - timeStart.millis
            totalMillis += elapsedMillis
        }

        return totalMillis / totalRequests
    }

}
