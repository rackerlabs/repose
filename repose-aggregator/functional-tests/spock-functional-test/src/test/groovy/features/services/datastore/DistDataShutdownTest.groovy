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
        repose.configurationProvider.applyConfigsRuntime("common", params)
        repose.configurationProvider.applyConfigsRuntime("features/services/datastore", params)
        repose.start()
        waitUntilReadyToServiceRequests()

        when: "i ask repose to stop"
        repose.stop()

        then: "the process should not be running"
        repose.isUp() == false
    }

    def "when configured with dist datastore as a filter should shutdown nicely when asked" () {
        given:
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigsRuntime("common", params)
        repose.configurationProvider.applyConfigsRuntime("features/filters/datastore", params)
        repose.start()
        waitUntilReadyToServiceRequests()

        when:
        repose.stop()

        then:
        repose.isUp() == false
    }

}
