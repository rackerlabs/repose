package features.core.connectionframework

import framework.ReposeConfigurationProvider
import framework.ReposeLogSearch
import framework.ReposeValveLauncher
import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain

/**
 *  Connection framework tests ported over from python
 */
class ConnectionFrameworkTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/core/connectionframework/")
        repose.start()

        sleep(5000)
    }

    def cleanupSpec() {
        if (repose)
                repose.stop()
        if (deproxy)
                deproxy.shutdown()
    }

    // Override configureReposeValve() to use the "apache" connection framework
    @Override
    def configureReposeValve() {

        ReposeConfigurationProvider reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configSamples)

        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                properties.getReposeEndpoint(),
                properties.getConfigDirectory(),
                properties.getReposePort(),
                properties.getReposeShutdownPort()
        )
        repose.enableDebug()
        reposeLogSearch = new ReposeLogSearch(logFile);
    }

    def "When accept header is absent"() {
        setup:
        MessageChain messageChain
        def headers = ["Host": "localhost:" + String.valueOf(repose.reposePort), "User-Agent": deproxy.VERSION_STRING]

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/", headers: headers, addDefaultHeaders: false)

        then:
        !messageChain.handlings[0].request.headers.contains("accept")
    }

    def "When accept header is empty"() {
        setup:
        MessageChain messageChain
        def headers = ["Host": "localhost:" + String.valueOf(repose.reposePort), "User-Agent": deproxy.VERSION_STRING,
                "Accept": ""]

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/", headers: headers, addDefaultHeaders: false)

        then:
        messageChain.handlings[0].request.headers.getFirstValue("accept") == null
    }

    def "When accept header is asterisks"() {
        setup:
        MessageChain messageChain
        def headers = ["Host": "localhost:" + String.valueOf(repose.reposePort), "User-Agent": deproxy.VERSION_STRING,
                "Accept": "*/*"]

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/", headers: headers, addDefaultHeaders: false)

        then:
        messageChain.handlings[0].request.headers.getFirstValue("accept").equals("*/*")
    }

    def "When accept header is type asterisk"() {
        setup:
        MessageChain messageChain
        def headers = ["Host": "localhost:" + String.valueOf(repose.reposePort), "User-Agent": deproxy.VERSION_STRING,
                "Accept": "text/*"]

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/", headers: headers, addDefaultHeaders: false)

        then:
        messageChain.handlings[0].request.headers.getFirstValue("accept").equals("text/*")
    }

    def "When accept header is subtype"() {
        setup:
        MessageChain messageChain
        def headers = ["Host": "localhost:" + String.valueOf(repose.reposePort), "User-Agent": deproxy.VERSION_STRING,
                "Accept": "text/plain"]

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/", headers: headers, addDefaultHeaders: false)

        then:
        messageChain.handlings[0].request.headers.getFirstValue("accept").equals("text/plain")
    }
}
