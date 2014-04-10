package features.services.datastore

import framework.ReposeLogSearch
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.PortFinder
import spock.lang.Ignore
import spock.lang.Unroll

/**
 * Created by jennyvo on 4/9/14.
 */
class DistDataStoreMisConfigTest extends ReposeValveTest{
    static def datastoreEndpoint1
    def searchError = "Configuration update error. Reason: Validation error on resource"
    def searchReasion = "port-config"

    @Unroll
    def "When start data store config #configuration"() {
        given:
        searchReasion = searchMsg
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
        repose.configurationProvider.applyConfigs("features/services/datastore/"+configuration, params)

        when:
        repose.start()
        waitUntilReadyToServiceRequests("503")

        then:
        logSearch.searchByString(searchError).size() > 0
        logSearch.searchByString(searchReasion).size() > 0
        logSearch.searchByString("NullPointerException").size() == 0

        where:
        configuration                   |searchMsg
        "noportconfig"                  |"port-config"
        "noportelement"                 |"The content of element 'port-config' is not complete"
        "noportattribute"               |"Attribute 'port' must appear on element 'port'"
    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()

        if (repose)
            repose.stop()

    }
}
