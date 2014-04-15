package features.filters.slf4jlogging

import framework.ReposeLogSearch
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

/**
 * Created by jennyvo on 4/8/14.
 */
class Slf4jHttpLoggingTest extends ReposeValveTest{
    def setupSpec() {
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/slf4jhttplogging", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

    }

    def "Test check slf4log" () {
        def logSearch = new ReposeLogSearch(properties.logFile)
        logSearch.cleanLog()

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint)

        then:
        logSearch.searchByString("my-test-log") == 1
        logSearch.searchByString("my-special-log") == 1

    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }
}
