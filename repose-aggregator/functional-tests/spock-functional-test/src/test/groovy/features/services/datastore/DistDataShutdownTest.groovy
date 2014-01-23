package features.services.datastore

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy

class DistDataShutdownTest extends ReposeValveTest {

    def setupSpec(){
         deproxy = new Deproxy()
         deproxy.addEndpoint(properties.targetPort)
     }

     def cleanupSpec(){
         if (deproxy)
             deproxy.shutdown()
     }

    def "when configured with dist datastore as a service should shutdown nicely when asked" () {
        given: "repose is configured with dist datastore"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore", params)
        repose.start()
        waitUntilReadyToServiceRequests()

        when: "i ask repose to stop"
        repose.stop()

        then: "the process should not be running"
        repose.isUp() == false
    }
}
