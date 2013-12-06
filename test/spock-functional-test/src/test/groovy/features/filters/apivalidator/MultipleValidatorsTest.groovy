package features.filters.apivalidator

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy


class MultipleValidatorsTest extends ReposeValveTest{

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/apivalidator/jmeter/")
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }



}
