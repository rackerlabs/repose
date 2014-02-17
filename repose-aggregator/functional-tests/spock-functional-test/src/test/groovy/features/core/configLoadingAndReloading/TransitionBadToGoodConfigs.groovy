package features.core.configLoadingAndReloading

import framework.ReposeConfigurationProvider
import framework.ReposeLogSearch
import framework.ReposeValveLauncher
import framework.TestProperties
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import spock.lang.Specification
import spock.lang.Unroll

@Category(Slow.class)
class TransitionBadToGoodConfigs extends Specification {

    static int targetPort
    static Deproxy deproxy

    int reposePort
    int stopPort
    String url
    TestProperties properties
    ReposeConfigurationProvider reposeConfigProvider
    ReposeLogSearch reposeLogSearch
    ReposeValveLauncher repose
    Map params = [:]

    def setup() {

        properties = new TestProperties()
        this.reposePort = properties.reposePort
        this.stopPort = properties.reposeShutdownPort
        this.targetPort = properties.targetPort
        this.url = properties.reposeEndpoint

        params = properties.getDefaultTemplateParams()

        // start a deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(this.targetPort)

        // setup config provider
        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigTemplates())

        // set the common configs
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("features/core/configLoadingAndReloading/common", params)

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
    }

    @Unroll("start with bad #componentLabel configs, change to good, should get #expectedResponseCode")
    def "start with bad #componentLabel configs, change to good, should get #expectedResponseCode"() {

        given:
        // set the component-specific bad configs
        reposeConfigProvider.applyConfigs("features/core/configLoadingAndReloading/${componentLabel}-common", params)
        reposeConfigProvider.applyConfigs("features/core/configLoadingAndReloading/${componentLabel}-bad", params)

        // start repose
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(url, 120)


        expect: "starting Repose with good configs should yield 503's"
        deproxy.makeRequest(url: url).receivedResponse.code == "503"


        when: "the configs are changed to good ones and we wait for Repose to pick up the change"
        reposeConfigProvider.applyConfigs("features/core/configLoadingAndReloading/${componentLabel}-good", params)
        sleep 15000
        repose.waitForNon500FromUrl(url, 120)

        then: "Repose should start returning #expectedResponseCode"
        deproxy.makeRequest(url: url).receivedResponse.code == "${expectedResponseCode}"




        where:
        componentLabel            | expectedResponseCode
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
        "metrics"                 | 200
        "connectionPooling"       | 200
    }

    @Unroll("start with bad #componentLabel configs, change to good (for configs that lead to connection errors)")
    def "start with bad #componentLabel configs, change to good (for configs that lead to connection errors)"() {

        given:
        // set the component-specific bad configs
        reposeConfigProvider.applyConfigs("features/core/configLoadingAndReloading/${componentLabel}-common", params)
        reposeConfigProvider.applyConfigs("features/core/configLoadingAndReloading/${componentLabel}-bad", params)

        // start repose
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        sleep 35000


        when: "starting Repose with bad configs should lead to a connection exception"
        deproxy.makeRequest(url: url)

        then:
        thrown(ConnectException)


        when: "the configs are changed to good ones and we wait for Repose to pick up the change"
        reposeConfigProvider.applyConfigs("features/core/configLoadingAndReloading/${componentLabel}-good", params)
        sleep 35000

        then: "Repose should start returning 200's"
        deproxy.makeRequest(url: url).receivedResponse.code == "200"


        where:
        componentLabel | _
        "system-model" | _
        "container"    | _
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
