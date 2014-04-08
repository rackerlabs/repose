package features.filters.httplog.slf4jlogging

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy

/**
 * Created by jennyvo on 4/8/14.
 */
class Slf4jHttpLoggingTest extends ReposeValveTest{
    def setupSpec() {
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/httplogging", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }
}
