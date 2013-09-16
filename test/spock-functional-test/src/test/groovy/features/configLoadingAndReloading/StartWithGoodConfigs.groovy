package features.configLoadingAndReloading

import framework.ReposeConfigurationProvider
import framework.ReposeLogSearch
import framework.ReposeValveLauncher
import framework.TestProperties
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.PortFinder
import spock.lang.Specification
import spock.lang.Unroll

@Category(Slow.class)
class StartWithGoodConfigs extends Specification {

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

    def setup() {

        PortFinder pf = new PortFinder()
        this.reposePort = pf.getNextOpenPort() as int
        this.stopPort = pf.getNextOpenPort() as int
        this.targetPort = pf.getNextOpenPort() as int
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
        properties = new TestProperties(ClassLoader.getSystemResource("test.properties").openStream())
        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigSamples())

    }

    @Unroll
    def "start with good #componentLabel configs, should get #expectedResponseCode"() {

        given:
        // set the common and good configs
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigsRuntime(
                "features/configLoadingAndReloading/common",
                params)
        reposeConfigProvider.applyConfigsRuntime(
                "features/configLoadingAndReloading/${componentLabel}-common",
                params)
        reposeConfigProvider.applyConfigsRuntime(
                "features/configLoadingAndReloading/${componentLabel}-good",
                params)

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


        expect: "starting Repose with good configs should yield 200's"
        deproxy.makeRequest(url: url).receivedResponse.code == "${expectedResponseCode}"


        where:
        componentLabel            | expectedResponseCode
        "system-model"            | 200
        "container"               | 200
        "response-messaging"      | 200
        "rate-limiting"           | 200
        "versioning"              | 200
        "translation"             | 200
        "client-auth-n"           | 200
        "openstack-authorization" | 401
        "dist-datastore"          | 200
        "http-logging"            | 200
        "uri-identity"            | 200
        "header-identity"         | 200
        "ip-identity"             | 200
        "validator"               | 200
    }

    def cleanup() {
        if (repose) {
            repose.stop()
        }
        if (deproxy) {
            deproxy.shutdown()
        }
    }
}

