package features.core.proxy

import framework.ReposeValveTest
import framework.category.Bug
import org.rackspace.deproxy.Deproxy
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.MessageChain

/**
 * User: dimi5963
 * Date: 10/8/13
 * Time: 4:45 PM
 *
 * D-15731
 *
 * Client-request-logging attribute does not filter out logging information.  It is assigned to add request/response logging into log4j logs.  However, it maps to jetty properties.  Since we are not using Jetty but HttpClient, all requests are logged regardless of Client-request-logging attribute status.

 * Current workaround is to set the following properties in log4j.properties to anything but DEBU

 * log4j.logger.org.apache.commons.httpclient=WARN
 * log4j.logger.org.apache.http.wire=WARN

 * log4j.logger.org.apache.http.headers=WARN

 * Fix would map client-request-logging attribute to HttpClient
 */
@Category(Bug)
class ClientRequestLogging extends ReposeValveTest {

    def setupSpec() {
        cleanLogDirectory()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        params = properties.getDefaultTemplateParams()
    }

    static def params

    def cleanup() {
        if (repose) {
            repose.stop()
        }
    }

    def cleanupSpec() {
        deproxy.shutdown()
    }

    def "test with client request logging true"(){

        given: "Repose configs are updated"
        repose.configurationProvider.applyConfigsRuntime("common", params)
        repose.configurationProvider.applyConfigsRuntime("features/core/proxy/clientRequestLoggingTrue", params)
        repose.start()
        List<String> wire_logs = reposeLogSearch.searchByString("org.apache.http.wire")
        List<String> headers_logs = reposeLogSearch.searchByString("org.apache.http.headers")

        when:
        MessageChain messageChain = deproxy.makeRequest([url: reposeEndpoint, method: "GET"])
        List<String> after_wire_logs = reposeLogSearch.searchByString("org.apache.http.wire")
        List<String> after_headers_logs = reposeLogSearch.searchByString("org.apache.http.headers")

        then:
        after_wire_logs.size() - wire_logs.size() > 0
        after_headers_logs.size() - headers_logs.size() > 0


    }

    def "test with client request logging false"() {

        given: "Repose configs are updated"
        repose.configurationProvider.applyConfigsRuntime("common", params)
        repose.configurationProvider.applyConfigsRuntime("features/core/proxy/clientRequestLoggingFalse", params)
        repose.start()
        List<String> wire_logs = reposeLogSearch.searchByString("org.apache.http.wire")
        List<String> headers_logs = reposeLogSearch.searchByString("org.apache.http.headers")

        when:
        MessageChain messageChain = deproxy.makeRequest([url: reposeEndpoint, method: "GET"])
        List<String> after_wire_logs = reposeLogSearch.searchByString("org.apache.http.wire")
        List<String> after_headers_logs = reposeLogSearch.searchByString("org.apache.http.headers")

        then:
        after_wire_logs.size() - wire_logs.size() == 0
        after_headers_logs.size() - headers_logs.size() == 0

    }

    def "test with client request logging missing"(){

        given: "Repose configs are updated"
        repose.configurationProvider.applyConfigsRuntime("common", params)
        repose.configurationProvider.applyConfigsRuntime("features/core/proxy/clientRequestLoggingDNE", params)
        repose.start()
        List<String> wire_logs = reposeLogSearch.searchByString("org.apache.http.wire")
        List<String> headers_logs = reposeLogSearch.searchByString("org.apache.http.headers")

        when:
        MessageChain messageChain = deproxy.makeRequest([url: reposeEndpoint, method: "GET"])
        List<String> after_wire_logs = reposeLogSearch.searchByString("org.apache.http.wire")
        List<String> after_headers_logs = reposeLogSearch.searchByString("org.apache.http.headers")

        then:
        after_wire_logs.size() - wire_logs.size() == 0
        after_headers_logs.size() - headers_logs.size() == 0

    }
}
