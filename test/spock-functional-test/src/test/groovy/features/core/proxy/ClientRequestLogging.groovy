package features.core.proxy

import framework.ReposeValveTest
import framework.category.Bug
import org.rackspace.gdeproxy.Deproxy
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.MessageChain

/**
 * User: dimi5963
 * Date: 10/8/13
 * Time: 4:45 PM
 */
@Category(Bug)
class ClientRequestLogging extends ReposeValveTest {

    def setupSpec() {
        cleanLogDirectory()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    def "test with client request logging true"(){

        given: "Repose configs are updated"
        repose.updateConfigs("common","features/core/proxy/clientRequestLoggingTrue")
        repose.start()
        List<String> wire_logs = reposeLogSearch.searchByString("org.apache.http.wire")
        List<String> headers_logs = reposeLogSearch.searchByString("org.apache.http.headers")

        when:
        MessageChain messageChain = deproxy.makeRequest([url: reposeEndpoint, method: "GET"])
        wire_logs = wire_logs - reposeLogSearch.searchByString("org.apache.http.wire").size()
        headers_logs = headers_logs - reposeLogSearch.searchByString("org.apache.http.headers").size()

        then:
        wire_logs.size() > 0
        headers_logs.size() > 0


    }

    def "test with client request logging false"() {

        given: "Repose configs are updated"
        repose.updateConfigs("common","features/core/proxy/clientRequestLoggingFalse")
        repose.start()
        List<String> wire_logs = reposeLogSearch.searchByString("org.apache.http.wire")
        List<String> headers_logs = reposeLogSearch.searchByString("org.apache.http.headers")

        when:
        MessageChain messageChain = deproxy.makeRequest([url: reposeEndpoint, method: "GET"])
        wire_logs = wire_logs - reposeLogSearch.searchByString("org.apache.http.wire").size()
        headers_logs = headers_logs - reposeLogSearch.searchByString("org.apache.http.headers").size()

        then:
        wire_logs.size() == 0
        headers_logs.size() == 0


    }

    def "test with client request logging missing"(){

        given: "Repose configs are updated"
        repose.updateConfigs("common","features/core/proxy/clientRequestLoggingDNE")
        repose.start()
        List<String> wire_logs = reposeLogSearch.searchByString("org.apache.http.wire")
        List<String> headers_logs = reposeLogSearch.searchByString("org.apache.http.headers")

        when:
        MessageChain messageChain = deproxy.makeRequest([url: reposeEndpoint, method: "GET"])
        wire_logs = wire_logs - reposeLogSearch.searchByString("org.apache.http.wire").size()
        headers_logs = headers_logs - reposeLogSearch.searchByString("org.apache.http.headers").size()

        then:
        wire_logs.size() == 0
        headers_logs.size() == 0


    }
}
