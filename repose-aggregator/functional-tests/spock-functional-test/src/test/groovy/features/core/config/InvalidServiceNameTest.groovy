package features.core.config

import framework.ReposeConfigurationProvider
import framework.ReposeLogSearch
import framework.ReposeValveLauncher
import framework.TestProperties
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.PortFinder
import spock.lang.Specification
import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition


class InvalidServiceNameTest extends Specification {

    int reposePort
    int targetPort
    String url
    TestProperties properties
    ReposeConfigurationProvider reposeConfigProvider
    ReposeValveLauncher repose
    ReposeLogSearch reposeLogSearch
    Map params = [:]
    Deproxy deproxy
    String errorMessage = "cvc-enumeration-valid: Value 'not-a-service' is not facet-valid with respect to enumeration"
    boolean expectCleanShutdown

    def setup() {

        properties = new TestProperties()
        this.reposePort = properties.reposePort
        this.targetPort = properties.targetPort
        this.url = properties.reposeEndpoint

        int dataStorePort = PortFinder.Singleton.getNextOpenPort()
        params = properties.getDefaultTemplateParams()

        params += [
                'datastorePort' : dataStorePort,
        ]

        // start a deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(this.targetPort)

        // setup config provider
        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigTemplates())

    }


    def "start with invalid service name in system model configs, should log error and fail to connect"() {
        given:
        // set the common and good configs
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/core/config/common", params)
        reposeConfigProvider.applyConfigs("features/core/config/service-name-bad", params)
        expectCleanShutdown = false

        // start repose
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                url,
                properties.getConfigDirectory(),
                reposePort
        )
        repose.enableDebug()
        reposeLogSearch = new ReposeLogSearch(properties.getLogFile())
        reposeLogSearch.deleteLog()


        when: "starting Repose with an invalid service name"
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        then: "error should be logged"
        waitForCondition(repose.clock, "30s", "2s") {
            new File(reposeLogSearch.logFileLocation).exists()
        }
        waitForCondition(repose.clock, "20s", "2s"){
            reposeLogSearch.searchByString(errorMessage).size() != 0
        }

        when: "making a request to repose with and invalid service name"
        deproxy.makeRequest(url: url)
        then: "connection exception should be returned"
        thrown(ConnectException)

    }


    def "start with valid service name in system model configs, change to bad, should log the exception and get 200"() {
        given:
        // set the common and good configs
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/core/config/common", params)
        reposeConfigProvider.applyConfigs("features/core/config/service-name-good", params)
        expectCleanShutdown = true

        // start repose
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                url,
                properties.getConfigDirectory(),
                reposePort
        )
        repose.enableDebug()
        reposeLogSearch = new ReposeLogSearch(properties.getLogFile())
        reposeLogSearch.deleteLog()
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(url)

        expect: "starting Repose with good configs should yield 200"
        deproxy.makeRequest(url: url).receivedResponse.code == "200"


        when: "the configs are changed to have an invalid service and we wait for Repose to pick up the change"
        reposeConfigProvider.applyConfigs("features/core/config/service-name-bad", params)

        then: "error should be logged and Repose should still return 200"
        waitForCondition(repose.clock, "20s", "2s") {
            reposeLogSearch.searchByString(errorMessage).size() != 0
        }
        deproxy.makeRequest(url: url).receivedResponse.code == "200"

    }

    def cleanup() {
        if (repose) {
            repose.stop(throwExceptionOnKill: expectCleanShutdown)
        }
        if (deproxy) {
            deproxy.shutdown()
        }
    }
}

