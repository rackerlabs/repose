package features.filters.ipidentity

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain
import spock.lang.Unroll

class IpIdentityTest extends ReposeValveTest {

    def static String user1 = "reposeuser1"
    def static String user2 = "reposeuser2"
    def static Map headersIdSingle = ["x-header-user": user1, "some-other-header": "value"]
    def static Map headersIdMulti = ["x-header-user": user2 + "," + user1]

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/ipidentity")
        repose.start()
        waitUntilReadyToServiceRequests()
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def "when identifying requests by ip"() {

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]
        def user = ((Handling) sentRequest).request.headers.getFirstValue("x-pp-user");

        then: "Repose will send x-pp-user with a single value"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1

        and: "Repose will send x-pp-user based on requestor ip"
        user == "127.0.0.1;q=0.4" || user == "0:0:0:0:0:0:0:1;q=0.4" | user == "::1;q=0.4"

        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").contains("127.0.0.1;q=0.4")

        and: "Repose will send x-pp-groups with the value IP_Standard"
        ((Handling) sentRequest).request.headers.getFirstValue("x-pp-groups").equalsIgnoreCase("IP_Standard;q=0.4")

    }

    def "when identifying requests by x-forwarded-for"() {

        when: "Request contains x-forwarded-for"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, headers: ["x-forwarded-for": "10.6.51.192"]])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send x-pp-user with a single value"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1

        and: "Repose will send x-pp-user based on x-forwarded-for"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").contains("10.6.51.192;q=0.4")

        and: "Repose will send x-pp-groups with the value IP_Standard"
        ((Handling) sentRequest).request.headers.getFirstValue("x-pp-groups").equalsIgnoreCase("IP_Standard;q=0.4")
    }
}
