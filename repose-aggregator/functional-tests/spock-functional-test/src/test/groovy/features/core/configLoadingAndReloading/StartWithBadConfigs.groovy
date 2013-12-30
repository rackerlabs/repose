package features.core.configLoadingAndReloading

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

@Category(Slow.class)
class StartWithBadConfigs extends Specification {

    int reposePort
    int stopPort
    int targetPort
    String url
    TestProperties properties
    ReposeConfigurationProvider reposeConfigProvider
    ReposeLogSearch reposeLogSearch
    ReposeValveLauncher repose
    Map params = [:]
    Deproxy deproxy
    boolean expectCleanShutdown = true

    def setup() {

        this.reposePort = PortFinder.Singleton.getNextOpenPort() as int
        this.stopPort = PortFinder.Singleton.getNextOpenPort() as int
        this.targetPort = PortFinder.Singleton.getNextOpenPort() as int
        this.url = "http://localhost:${this.reposePort}/"

        params = [
                'reposePort': this.reposePort,
                'targetHostname': 'localhost',
                'targetPort': targetPort,
        ]

        // start a deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(this.targetPort)

        // setup config provider
        properties = new TestProperties()
        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigSamples())

    }

    @Unroll
    def "start with bad #componentLabel configs, should get 503"() {

        given:
        // set the common and good configs
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs(
                "features/core/configLoadingAndReloading/common",
                params)
        reposeConfigProvider.applyConfigs(
                "features/core/configLoadingAndReloading/${componentLabel}-common",
                params)
        reposeConfigProvider.applyConfigs(
                "features/core/configLoadingAndReloading/${componentLabel}-bad",
                params)
        expectCleanShutdown = true

        // start repose
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                url,
                properties.getConfigDirectory(),
                reposePort,
                stopPort
        )
        repose.enableDebug()
        reposeLogSearch = new ReposeLogSearch(properties.getLogFile());
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(url)


        expect: "starting Repose with good configs should yield 503's"
        deproxy.makeRequest(url: url).receivedResponse.code == "503"


        where:
        componentLabel            | _
        "response-messaging"      | _
        "rate-limiting"           | _
        "versioning"              | _
        "translation"             | _
        "client-auth-n"           | _
        "openstack-authorization" | _
        "dist-datastore"          | _
        "http-logging"            | _
        "uri-identity"            | _
        "header-identity"         | _
        "ip-identity"             | _
        "validator"               | _
    }


    @Unroll
    def "start with bad #componentLabel configs, should fail to connect"() {

        given:
        // set the common and good configs
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs(
                "features/core/configLoadingAndReloading/common",
                params)
        reposeConfigProvider.applyConfigs(
                "features/core/configLoadingAndReloading/${componentLabel}-common",
                params)
        reposeConfigProvider.applyConfigs(
                "features/core/configLoadingAndReloading/${componentLabel}-bad",
                params)
        expectCleanShutdown = false

        // start repose
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                url,
                properties.getConfigDirectory(),
                reposePort,
                stopPort
        )
        repose.enableDebug()
        reposeLogSearch = new ReposeLogSearch(properties.getLogFile());
        repose.start(killOthersBeforeStarting: false,
                     waitOnJmxAfterStarting: false)
        sleep 35000


        when: "starting Repose with bad configs should lead to a connection exception"
        deproxy.makeRequest(url: url)

        then:
        thrown(ConnectException)

        where:
        componentLabel            | _
        "system-model"            | _
        "container"               | _
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
