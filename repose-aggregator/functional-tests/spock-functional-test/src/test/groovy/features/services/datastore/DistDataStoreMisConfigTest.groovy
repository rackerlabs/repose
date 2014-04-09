package features.services.datastore

import framework.ReposeLogSearch
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.PortFinder

/**
 * Created by jennyvo on 4/9/14.
 */
class DistDataStoreMisConfigTest extends ReposeValveTest{
    static def datastoreEndpoint1
    def searchError = "Configuration update error. Reason: Validation error on resource"
    def searchReasion = "port-config"

    def "When start data store config without port-config"() {
        given:
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort1 = PortFinder.Singleton.getNextOpenPort()
        def logSearch = new ReposeLogSearch(properties.logFile)
        logSearch.cleanLog()

        datastoreEndpoint1 = "http://localhost:${dataStorePort1}"

        def params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort1' : dataStorePort1
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/noportconfig", params)

        when:
        repose.start()
        waitUntilReadyToServiceRequests("503")

        then:
        logSearch.searchByString(searchError).size() > 0
        logSearch.searchByString(searchReasion).size() > 0
        logSearch.searchByString("NullPointerException").size() == 0

    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()

        if (repose)
            repose.stop()

    }
}
