package features.configLoadingAndReloading

import framework.ReposeConfigurationProvider
import framework.ReposeLogSearch
import framework.ReposeValveLauncher
import framework.category.Slow
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.PortFinder
import spock.lang.Specification
import spock.lang.Unroll

@Category(Slow.class)
class StartWithBadConfigs extends Specification {

    int reposePort
    int stopPort
    int targetPort
    String url
    Properties properties
    def configDirectory
    ReposeConfigurationProvider reposeConfigProvider
    def logFile
    ReposeLogSearch reposeLogSearch
    ReposeValveLauncher repose
    Map params = [:]
    Deproxy deproxy
    boolean expectCleanShutdown = true

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
        properties = new Properties()
        properties.load(ClassLoader.getSystemResource("test.properties").openStream())
        logFile = properties.getProperty("repose.log")
        configDirectory = properties.getProperty("repose.config.directory")
        def configSamples = properties.getProperty("repose.config.samples")
        reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configSamples)

    }

    @Unroll
    def "start with bad #componentLabel configs, should get 503"() {

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
                "features/configLoadingAndReloading/${componentLabel}-bad",
                params)
        expectCleanShutdown = true

        // start repose
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getProperty("repose.jar"),
                url,
                configDirectory,
                reposePort,
                stopPort
        )
        repose.enableDebug()
        reposeLogSearch = new ReposeLogSearch(logFile);
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(url)


        expect: "starting Repose with good configs should yield 503's"
        deproxy.makeRequest(url: url).receivedResponse.code == "503"


        where:
        componentLabel            | _
//        "response-messaging"      | _
//        "rate-limiting"           | _
//        "versioning"              | _
        "translation"             | _
//        "client-auth-n"           | _
//        "openstack-authorization" | _
//        "dist-datastore"          | _
//        "http-logging"            | _
//        "uri-identity"            | _
//        "header-identity"         | _
//        "ip-identity"             | _
//        "validator"               | _
    }

//
//    @Unroll
//    def "start with bad #componentLabel configs, should fail to connect"() {
//
//        given:
//        // set the common and good configs
//        reposeConfigProvider.cleanConfigDirectory()
//        reposeConfigProvider.applyConfigsRuntime(
//                "features/configLoadingAndReloading/common",
//                params)
//        reposeConfigProvider.applyConfigsRuntime(
//                "features/configLoadingAndReloading/${componentLabel}-common",
//                params)
//        reposeConfigProvider.applyConfigsRuntime(
//                "features/configLoadingAndReloading/${componentLabel}-bad",
//                params)
//        expectCleanShutdown = false
//
//        // start repose
//        repose = new ReposeValveLauncher(
//                reposeConfigProvider,
//                properties.getProperty("repose.jar"),
//                url,
//                configDirectory,
//                reposePort,
//                stopPort
//        )
//        repose.enableDebug()
//        reposeLogSearch = new ReposeLogSearch(logFile);
//        repose.start(killOthersBeforeStarting: false,
//                     waitOnJmxAfterStarting: false)
//        sleep 35000
//
//
//        when: "starting Repose with bad configs should lead to a connection exception"
//        deproxy.makeRequest(url: url)
//
//        then:
//        thrown(ConnectException)
//
//        where:
//        componentLabel            | _
//        "system-model"            | _
//        "container"               | _
//    }

    def cleanup() {
        if (repose) {
            repose.stop(throwExceptionOnKill: expectCleanShutdown)
        }
        if (deproxy) {
            deproxy.shutdown()
        }
    }
}
