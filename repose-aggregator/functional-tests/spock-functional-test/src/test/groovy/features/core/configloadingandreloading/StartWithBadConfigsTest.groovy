package features.core.configloadingandreloading

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.TestProperties
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import spock.lang.Specification
import spock.lang.Unroll

@Category(Slow.class)
class StartWithBadConfigsTest extends Specification {

    int reposePort
    int targetPort
    String url
    TestProperties properties
    ReposeConfigurationProvider reposeConfigProvider
    ReposeValveLauncher repose
    Map params = [:]
    Deproxy deproxy
    boolean expectCleanShutdown = true

    def setup() {

        properties = new TestProperties()
        this.reposePort = properties.reposePort
        this.targetPort = properties.targetPort
        this.url = properties.reposeEndpoint

        params = properties.getDefaultTemplateParams()

        // start a deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(this.targetPort)

        // setup config provider
        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigTemplates())

    }

    @Unroll("start with bad #componentLabel configs, should get 503")
    def "start with bad #componentLabel configs, should get 503"() {

        given:
        // set the common and good configs
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("features/core/configloadingandreloading/common", params)
        reposeConfigProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-common", params)
        reposeConfigProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-bad", params)
        expectCleanShutdown = true

        // start repose
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getServoJar(),
                properties.getJettyJar(),
                properties.getReposeWar(),
                url,
                properties.getConfigDirectory(),
                reposePort
        )
        repose.enableDebug()
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForDesiredResponseCodeFromUrl(url, [503])


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
        "uri-identity"            | _
        "header-identity"         | _
        "ip-identity"             | _
        "validator"               | _
        "metrics"                 | _
        "connectionPooling"       | _
    }


    @Unroll("start with bad #componentLabel configs, should fail to connect")
    def "start with bad #componentLabel configs, should fail to connect"() {

        given:
        // set the common and good configs
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("features/core/configloadingandreloading/common", params)
        reposeConfigProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-common", params)
        reposeConfigProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-bad", params)
        expectCleanShutdown = false

        // start repose
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getServoJar(),
                properties.getJettyJar(),
                properties.getReposeWar(),
                url,
                properties.getConfigDirectory(),
                reposePort
        )
        repose.enableDebug()
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
