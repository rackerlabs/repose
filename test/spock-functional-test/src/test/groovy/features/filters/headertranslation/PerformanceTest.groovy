package features.filters.headertranslation
import framework.ReposeValveTest
import framework.category.Flaky
import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain

@Category(Flaky)
class PerformanceTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanupSpec() {
        deproxy.shutdown()
    }


    def "performance test configs produce expected responses"() {
        when: "I make a request through header translation filter"
        repose.applyConfigs( "features/filters/headertranslation/common")
        repose.start()
        MessageChain mcHdrXlate = deproxy.makeRequest(reposeEndpoint, "GET", ["X-OneToMany-A":"12345", "X-OneToMany-B":"abcde"])
        repose.stop()

        and: "I make a request through header translation filter"
        repose.applyConfigs( "features/filters/headertranslation/perftest")
        repose.start()
        MessageChain mcTranslation = deproxy.makeRequest(reposeEndpoint, "GET", ["X-OneToMany-A":"12345", "X-OneToMany-B":"abcde"])
        repose.stop()

        then: "Headers are translated correctly by HDR XLATE"
        def mcHdrHandling = mcHdrXlate.handlings.get(0)
        mcHdrHandling.request.headers.contains("X-OneToMany-A")
        mcHdrHandling.request.headers.contains("X-OneToMany-C")
        mcHdrHandling.request.headers.contains("X-OneToMany-D")
        mcHdrHandling.request.headers.getFirstValue("X-OneToMany-C") == mcHdrXlate.sentRequest.headers.getFirstValue("X-OneToMany-A")
        mcHdrHandling.request.headers.getFirstValue("X-OneToMany-D") == mcHdrXlate.sentRequest.headers.getFirstValue("X-OneToMany-A")

        then: "Headers are translated correctly by Translation Filter"
        def mcTransHandling = mcTranslation.handlings.get(0)
        mcTransHandling.request.headers.contains("X-OneToMany-A")
        mcTransHandling.request.headers.contains("X-OneToMany-C")
        mcTransHandling.request.headers.contains("X-OneToMany-D")
        mcTransHandling.request.headers.getFirstValue("X-OneToMany-C") == mcTranslation.sentRequest.headers.getFirstValue("X-OneToMany-A")
        mcTransHandling.request.headers.getFirstValue("X-OneToMany-D") == mcTranslation.sentRequest.headers.getFirstValue("X-OneToMany-A")
    }

    def "performance test to ensure header translation is on par or better than translation filter"() {
        given:
        int totalRequests = 1000

        when: "I make 100 requests through header translation filter"
        repose.applyConfigs( "features/filters/headertranslation/common")
        repose.start()
        def averageWithHdrXlate = makeRequests(totalRequests)
        repose.stop()

        and: "I make 100 requests through translation filter"
        repose.applyConfigs( "features/filters/headertranslation/perftest")
        repose.start()
        def averageWithTranslationFilter = makeRequests(totalRequests)
        repose.stop()

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
