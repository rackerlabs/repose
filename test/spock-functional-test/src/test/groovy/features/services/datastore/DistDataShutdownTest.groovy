package features.services.datastore

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy

class DistDataShutdownTest extends ReposeValveTest {
    def setup(){
         deproxy = new Deproxy()
         deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
     }

     def cleanup(){
         if (deproxy)
             deproxy.shutdown()
     }

    def "when configured with dist datastore as a service should shutdown nicely when asked" () {
        given: "repose is configured with dist datastore"
        repose.applyConfigs("features/services/datastore/")
        repose.start()
        waitUntilReadyToServiceRequests()

        when: "i ask repose to stop"
        repose.stop()

        then: "the process should not be running"
        repose.isUp() == false
    }

    def "when configured with dist datastore as a filter should shutdown nicely when asked" () {
        given:
        repose.applyConfigs("features/filters/datastore/")
        repose.start()
        waitUntilReadyToServiceRequests()

        when:
        repose.stop()

        then:
        repose.isUp() == false
    }

}
