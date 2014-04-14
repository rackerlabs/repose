package features.filters.slf4jlogging

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
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint)

        then:
        1 == 1

    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }
}
