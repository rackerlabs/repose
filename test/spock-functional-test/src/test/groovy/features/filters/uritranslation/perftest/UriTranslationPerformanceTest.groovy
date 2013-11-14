package features.filters.uritranslation.perftest

import framework.ReposeValveTest
import framework.category.Benchmark
import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

@Category(Benchmark.class)
class UriTranslationPerformanceTest extends ReposeValveTest {


    def "performance test to ensure header translation is on par or better than translation filter"() {
        given:
        int totalThreads = 10

        when: "I make 1000 requests through the uri stripper filter"
        repose.applyConfigs("features/filters/uristripper/common", "features/filters/uristripper/nolocationrewrite")
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
        def averageWithHdrXlate = makeRequests(totalThreads)
        repose.stop()
        deproxy.shutdown()

        and: "I make 1000 requests through translation filter"
        repose.applyConfigs("features/filters/translation/common", "features/filters/uritranslation/perftest")
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
        def averageWithTranslationFilter = makeRequests(totalThreads)
        repose.stop()
        deproxy.shutdown()

        then: "Average response time for uri stripper is <= avg response time for translation filter"
        println("URI Stripper: " + averageWithHdrXlate + " Translation: " + averageWithTranslationFilter)
        averageWithHdrXlate <= averageWithTranslationFilter * 1.1
    }


    def makeRequests(int numThreads=10, numRequests=100) {
        def resp = { request -> return new Response(301, "Moved Permanently") }

        // warm up, ignore response times
        for (int i : 1..numThreads) {
            deproxy.makeRequest([url: reposeEndpoint + "/v1/12345/path/to/resource", defaultHandler: resp])
        }

        def threads = []
        def elapsedTimes = []

        for (int i : 1..numThreads) {

            def th = new Thread({
                long totalMillis = 0
                for (int j : 1..numRequests) {
                    // start time
                    def timeStart = new DateTime()
                    MessageChain mc = deproxy.makeRequest([url: reposeEndpoint + "/v1/12345/path/to/resource", defaultHandler: resp])
                    def timeStop = new DateTime()
                    def elapsedMillis = timeStop.millis - timeStart.millis
                    totalMillis += elapsedMillis
                }
                elapsedTimes << totalMillis

            })

            threads << th
        }

        threads.each { it.start() }
        threads.each { it.join() }

        assert elapsedTimes.size() == numThreads
        long total = 0;
        elapsedTimes.each { total += it }

        // now let's capture response times

        return total / (numThreads*numRequests)
    }

}
