package features.core.config

import framework.ReposeConfigurationProvider
import framework.ReposeLogSearch
import framework.ReposeValveLauncher
import framework.TestProperties
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.PortFinder

import spock.lang.Specification
import spock.lang.Unroll

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

@Category(Slow)
class DefaultDestinationTest extends Specification {

    int reposePort
    int targetPort

    String url
    TestProperties properties
    ReposeConfigurationProvider reposeConfigProvider
    ReposeValveLauncher repose
    ReposeLogSearch reposeLogSearch
    Map params = [:]
    Deproxy deproxy
    boolean expectCleanShutdown

    String errorMessage="There should only be one default destination."

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

    @Unroll("when defaults: #default1, #default2, #default3")
    def "start with more or less than one default destination endpoint in system model configs, should log error and fail to connect"() {
        given:
        // set the common and good configs
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/core/config/common", params)

        params += [
                "default1":default1, "default2":default2, "default3":default3

        ]
        reposeConfigProvider.applyConfigs("features/core/config/default-dest", params)

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


        when: "starting Repose with more or less than one default destination endpoint"
        repose.start([waitOnJmxAfterStarting: false])

        then: "error should be logged"
        waitForCondition(repose.clock, "30s", "2s") {
            new File(reposeLogSearch.logFileLocation).exists()
        }
        waitForCondition(repose.clock, "20s", "2s"){
            reposeLogSearch.searchByString(errorMessage).size() != 0
        }

        when: "making a request to repose with and invalid default destination endpoint settings"
        deproxy.makeRequest(url: url)
        then: "connection exception should be returned"
        thrown(ConnectException)


        where:
        default1 | default2 | default3
        false    | false    | false
        true     | true     | true
        true     | true     | false
        true     | false    | true
        false    | true     | true

    }

    @Unroll("when defaults: #default1, #default2, #default3")
    def "start with only one default destination endpoint in system model configs, should return 200"() {
        given:
        // set the common and good configs
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/core/config/common", params)

        params += [
                "default1":default1, "default2":default2, "default3":default3

        ]
        reposeConfigProvider.applyConfigs("features/core/config/default-dest", params)
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

        where:
        default1  | default2  | default3
        true      | false     | false
        false     | true      | false
        false     | false     | true

    }

    @Unroll("when defaults: #default1, #default2, #default3")
    def "start with more or less than one default destination and null values, should log error and fail to connect"() {
        given:
        // set the common and good configs
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/core/config/common", params)

        params += [
                "default1":defaultParamWrapper(default1),
                "default2":defaultParamWrapper(default2),
                "default3":defaultParamWrapper(default3)
        ]
        reposeConfigProvider.applyConfigs("features/core/config/default-dest-null", params)

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


        when: "starting Repose with more or less than one default destination endpoint"
        repose.start([waitOnJmxAfterStarting: false])

        then: "error should be logged"
        waitForCondition(repose.clock, "30s", "2s") {
            new File(reposeLogSearch.logFileLocation).exists()
        }
        waitForCondition(repose.clock, "20s", "2s"){
            reposeLogSearch.searchByString(errorMessage).size() != 0
        }

        when: "making a request to repose with and invalid default destination endpoint settings"
        deproxy.makeRequest(url: url)
        then: "connection exception should be returned"
        thrown(ConnectException)


        where:
        default1 | default2 | default3
        null     | null     | null
        null     | null     | false
        null     | false    | false
        null     | true     | true
        null     | false    | null
        true     | null     | true
        true     | true     | null
        false    | null     | null
        false    | null     | false
        false    | false    | null
    }

    private def defaultParamWrapper(Object value){
        return value==null ? '' :  '\" default=\"'+value
    }


    def cleanup() {
        if (repose) {
            repose.stop([throwExceptionOnKill: false])
        }
        if (deproxy) {
            deproxy.shutdown()
        }
    }
}
