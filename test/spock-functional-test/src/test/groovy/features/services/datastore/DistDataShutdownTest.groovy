package features.services.datastore

import framework.ReposeValveTest

class DistDataShutdownTest extends ReposeValveTest {


    def "when configured with dist datastore as a service, should shutdown nicely when asked" () {
        given:
        repose.applyConfigs("features/services/datastore/")
        repose.start()

        when:
        repose.stop()

        then:
        repose.isUp() == false
    }

    def "when configured with dist datastore as a filter, should shutdown nicely when asked" () {
        given:
        repose.applyConfigs("features/filters/datastore/")
        repose.start()

        when:
        repose.stop()

        then:
        repose.isUp() == false
    }

}
