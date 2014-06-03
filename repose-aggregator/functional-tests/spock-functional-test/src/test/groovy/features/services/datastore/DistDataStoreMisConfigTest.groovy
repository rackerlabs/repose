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

import static framework.TestUtils.timedSearch

/**
 * Created by jennyvo on 4/9/14.
 */
@Category(Slow)
class DistDataStoreMisConfigTest extends ReposeValveTest{
    static def datastoreEndpoint


    @Unroll("When start data store config #configuration")
    def "Test data store with wrong config"() {
        given:
        def searchError = "Configuration update error. Reason: Validation error on resource"
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort = PortFinder.Singleton.getNextOpenPort()
        reposeLogSearch.deleteLog()

        datastoreEndpoint = "http://localhost:${dataStorePort}"

        def params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort' : dataStorePort
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/"+configuration, params)

        when:
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests("503")

        then:
        reposeLogSearch.searchByString("NullPointerException").size() == 0
        timedSearch(10) {
            reposeLogSearch.searchByString(searchError).size() > 0
        }
        timedSearch(10) {
            reposeLogSearch.searchByString(searchMsg).size() > 0
        }

        where:
        configuration                   |searchMsg
        "noportconfig"                  |"port-config"
        "noportelement"                 |"The content of element 'port-config' is not complete"
        "noportattribute"               |"Attribute 'port' must appear on element 'port'"
        "noclusterattribute"            |"Attribute 'cluster' must appear on element 'port'"

    }

    @Unroll("When start data store mismatch config #configuration")
    def "Test data store with mismatch config"() {
        given:
        def searchError = "Configuration update error. Reason: port out of range:-1"
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort = PortFinder.Singleton.getNextOpenPort()
        reposeLogSearch.deleteLog()

        datastoreEndpoint = "http://localhost:${dataStorePort}"

        def params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort' : dataStorePort
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/"+configuration, params)

        when:
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests("503")

        then:
        reposeLogSearch.searchByString("NullPointerException").size() == 0
        timedSearch(10) {
            reposeLogSearch.searchByString(searchError).size() > 0
        }

        where:
        configuration << ["clustermismatch","nodemismatch"]

    }

    @Unroll("When start data store with port out of range: #port")
    def "Test data store with port out of range"() {
        given:
        def searchError = "port out of range:"+port
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort = port
        reposeLogSearch.deleteLog()

        datastoreEndpoint = "http://localhost:${dataStorePort}"

        def params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort' : dataStorePort
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/portranges", params)

        when:
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests("503")

        then:
        reposeLogSearch.searchByString("NullPointerException").size() == 0
        timedSearch(10) {
            reposeLogSearch.searchByString(searchError).size() > 0
        }

        where:
        port    << [65536,-1]
    }

    @Unroll("When start data store with reserved: #port")
    def "Test start data store with reserved ports"() {
        given:
        def searchError = "Unable to start Distributed Datastore Jetty Instance: Permission denied"
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort = port
        reposeLogSearch.deleteLog()

        datastoreEndpoint = "http://localhost:${dataStorePort}"

        def params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort' : dataStorePort
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/portranges", params)

        when:
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests("503", false)

        then:
        reposeLogSearch.searchByString("NullPointerException").size() == 0
        timedSearch(10) {
            reposeLogSearch.searchByString(searchError).size() > 0
        }

        where:
        port << [21, 22, 23, 1023]

    }

    def "When start data store port conflict"() {
        given:
        def searchError = "java.net.BindException: Address already in use"
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        int dataStorePort = PortFinder.Singleton.getNextOpenPort()
        reposeLogSearch.deleteLog()

        datastoreEndpoint = "http://localhost:${dataStorePort}"

        def params = properties.getDefaultTemplateParams()
        params += [
                'datastorePort' : dataStorePort
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/portconflict", params)

        when:
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests("503", false)

        then:
        reposeLogSearch.searchByString("NullPointerException").size() == 0
        timedSearch(10) {
            reposeLogSearch.searchByString(searchError).size() > 0
        }

    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()

        if (repose)
            repose.stop([throwExceptionOnKill: false])

    }
}
