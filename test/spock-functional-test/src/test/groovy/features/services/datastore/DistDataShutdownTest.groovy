package features.services.datastore

import framework.ReposeValveTest

class DistDataShutdownTest extends ReposeValveTest {

    def "when configured with dist datastore as a service should shutdown nicely when asked" () {
        given: "repose is configured with dist datastore"
        repose.applyConfigs("features/services/datastore/")
        repose.start()

        when: "i ask repose to stop"
        sleep(15000)  // sleeping here as repose may not have been up
        repose.stop()

        then: "the process should not be running"
        repose.isUp() == false
    }

    def "when configured with dist datastore as a filter should shutdown nicely when asked" () {
        given:
        repose.applyConfigs("features/filters/datastore/")
        repose.start()

        when:
        sleep(15000)  // sleeping here as repose may not have been up
        repose.stop()

        then:
        repose.isUp() == false
    }

}
