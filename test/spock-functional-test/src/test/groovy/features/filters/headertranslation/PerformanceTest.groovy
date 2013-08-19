package features.filters.headertranslation

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy


class PerformanceTest extends ReposeValveTest {

    def "performance test to ensure header translation is on par or better than translation filter"() {

        when: "I make 100 requests through header translation filter"
        repose.applyConfigs( "features/filters/headertranslation/common",
                "features/filters/headertranslation/oneToMany" )
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
        def averageWithHdrXlate = makeRequests(100)
        repose.stop()
        deproxy.shutdown()

        and: "I make 100 requests through translation filter"
        repose.applyConfigs( "features/filters/headertranslation/perftest")
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
        def averageWithTranslationFilter = makeRequests(100)
        repose.stop()
        deproxy.shutdown()

        then: "Average response time for header translation is <= avg response time for translation filter"
        averageWithHdrXlate <= averageWithTranslationFilter
    }


    def makeRequests(int totalRequests) {
        for (int i : totalRequests) {
            // make request
            // capture time
            // add it to average time
        }

        // return average
    }

}
