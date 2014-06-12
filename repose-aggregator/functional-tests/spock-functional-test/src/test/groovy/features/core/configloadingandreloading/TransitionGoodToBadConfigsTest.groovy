package features.core.configloadingandreloading

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
class TransitionGoodToBadConfigsTest extends Specification {

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

        properties = new TestProperties()
        this.reposePort = properties.reposePort
        this.stopPort = properties.reposeShutdownPort
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

    @Unroll("start with good #componentLabel configs, change to bad, should still get #expectedResponseCode")
    def "start with good #componentLabel configs, change to bad, should still get #expectedResponseCode"() {

        given:
        // set the common and good configs
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("features/core/configloadingandreloading/common", params)
        reposeConfigProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-common", params)
        reposeConfigProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-good", params)

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


        expect: "starting Repose with good configs should yield #expectedResponseCode"
        deproxy.makeRequest(url: url).receivedResponse.code == "${expectedResponseCode}"


        when: "the configs are changed to bad ones and we wait for Repose to pick up the change"
        reposeConfigProvider.applyConfigs(
                "features/core/configloadingandreloading/${componentLabel}-bad",
                params)
        sleep 15000

        then: "Repose should still return #expectedResponseCode"
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
        "uri-identity"            | 200
        "header-identity"         | 200
        "ip-identity"             | 200
        "validator"               | 200
        "metrics"                 | 200
        "connectionPooling"       | 200
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

