package features.core.connectionframework

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

/**
 *  Connection framework tests ported over from python
 */
class ConnectionFrameworkTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getReposeProperty("target.port").toInteger())

        repose.applyConfigs("features/core/connectionframework/")
        repose.start()

        waitUntilReadyToServiceRequests()
    }

    def cleanupSpec() {
        if (repose)
                repose.stop()
        if (deproxy)
                deproxy.shutdown()
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
