package features.services.datastore

import framework.ReposeLogSearch
import framework.ReposeValveTest
import framework.category.Bug
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.PortFinder
import spock.lang.Ignore
import spock.lang.Unroll

/**
 * Created by jennyvo on 4/9/14.
 */
@Category(Slow)
class DistDataStoreMisConfigTest extends ReposeValveTest{
    static def datastoreEndpoint
    def searchError = ""
    def searchReason = ""

    @Unroll
    def "When start data store config #configuration"() {
        given:
        searchError = "Configuration update error. Reason: Validation error on resource"
        searchReason = searchMsg
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort = PortFinder.Singleton.getNextOpenPort()
        def logSearch = new ReposeLogSearch(properties.logFile)
        logSearch.deleteLog()

        datastoreEndpoint = "http://localhost:${dataStorePort}"

        def params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort' : dataStorePort
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/"+configuration, params)

        when:
        repose.start()
        waitUntilReadyToServiceRequests("503")

        then:
        logSearch.searchByString("NullPointerException").size() == 0
        logSearch.searchByString(searchError).size() > 0
        logSearch.searchByString(searchReason).size() > 0

        where:
        configuration                   |searchMsg
        "noportconfig"                  |"port-config"
        "noportelement"                 |"The content of element 'port-config' is not complete"
        "noportattribute"               |"Attribute 'port' must appear on element 'port'"
        "noclusterattribute"            |"Attribute 'cluster' must appear on element 'port'"

    }

    @Unroll
    def "When start data store mismatch config #configuration"() {
        given:
        def searchError = "Configuration update error. Reason: port out of range:-1"
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort = PortFinder.Singleton.getNextOpenPort()
        def logSearch = new ReposeLogSearch(properties.logFile)
        logSearch.deleteLog()

        datastoreEndpoint = "http://localhost:${dataStorePort}"

        def params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort' : dataStorePort
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/"+configuration, params)

        when:
        repose.start()
        waitUntilReadyToServiceRequests("503")

        then:
        logSearch.searchByString("NullPointerException").size() == 0
        logSearch.searchByString(searchError).size() > 0

        where:
        configuration << ["clustermismatch","nodemismatch"]

    }

    @Unroll
    def "When start data store with port out of range: #port"() {
        given:
        def searchError = "port out of range:"+port
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort = port
        def logSearch = new ReposeLogSearch(properties.logFile)
        logSearch.deleteLog()

        datastoreEndpoint = "http://localhost:${dataStorePort}"

        def params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort' : dataStorePort
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/portranges", params)

        when:
        repose.start()
        waitUntilReadyToServiceRequests("503")

        then:
        logSearch.searchByString("NullPointerException").size() == 0
        logSearch.searchByString(searchError).size() > 0

        where:
        port    << [65536,-1]
    }

    @Unroll
    def "When start data store with reserved: #port"() {
        given:
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort = port
        def logSearch = new ReposeLogSearch(properties.logFile)
        logSearch.deleteLog()

        datastoreEndpoint = "http://localhost:${dataStorePort}"

        def params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort' : dataStorePort
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/portranges", params)

        when:
        repose.start()
        waitUntilReadyToServiceRequests("503", false)

        then:
        logSearch.searchByString("NullPointerException").size() == 0
        logSearch.searchByString(searchError).size() > 0

        where:
        port                    |searchError
        21                      |"Unable to start Distributed Datastore Jetty Instance: Permission denied"
        22                      |"Unable to start Distributed Datastore Jetty Instance: Permission denied"
        1023                    |"Unable to start Distributed Datastore Jetty Instance: Permission denied"

    }

    def "When start data store port conflict"() {
        given:
        def searchError = "java.net.BindException: Address already in use"
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort = PortFinder.Singleton.getNextOpenPort()
        def logSearch = new ReposeLogSearch(properties.logFile)
        logSearch.deleteLog()

        datastoreEndpoint = "http://localhost:${dataStorePort}"

        def params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort' : dataStorePort
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/portconflict", params)

        when:
        repose.start()
        waitUntilReadyToServiceRequests("503", false)

        then:
        logSearch.searchByString("NullPointerException").size() == 0
        logSearch.searchByString(searchError).size() > 0

    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()

        if (repose)
            repose.stop([throwExceptionOnKill: false])

    }
}
