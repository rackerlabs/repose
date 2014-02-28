package features.services.datastore

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.PortFinder

class DistDataShutdownTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()
    }

    def "when configured with dist datastore as a service should shutdown nicely when asked"() {
        given: "repose is configured with dist datastore"
        def params = properties.getDefaultTemplateParams()
        int dataStorePort1 = PortFinder.Singleton.getNextOpenPort()
        int dataStorePort2 = PortFinder.Singleton.getNextOpenPort()
        params += [
                'datastorePort1': dataStorePort1,
                'datastorePort2': dataStorePort2
        ]
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
