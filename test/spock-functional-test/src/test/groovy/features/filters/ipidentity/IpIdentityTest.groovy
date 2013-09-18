package features.filters.ipidentity

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.PortFinder
import spock.lang.Shared

@Category(Slow.class)
class IpIdentityTest extends ReposeValveTest {

    @Shared
    def url
    def setupSpec() {
        PortFinder pf = new PortFinder()
        int deproxyPort = pf.getNextOpenPort()
        int reposePort = pf.getNextOpenPort()
        deproxy = new Deproxy()
        deproxy.addEndpoint(deproxyPort)

        url = "http://localhost:${reposePort}"

        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigsRuntime(
                "common",
                ["reposePort": reposePort.toString(),
                        "targetPort": deproxyPort.toString()]);
        repose.configurationProvider.applyConfigsRuntime(
                "features/filters/ipidentity",
                ["reposePort": reposePort.toString(),
                        "targetPort": deproxyPort.toString()]);
        repose.start()
        repose.waitForNon500FromUrl(url)
//        waitUntilReadyToServiceRequests()
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
        def messageChain = deproxy.makeRequest([url: url])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]
        def user = ((Handling) sentRequest).request.headers.getFirstValue("x-pp-user");

        then: "Repose will send x-pp-user with a single value"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1

        and: "Repose will send x-pp-user based on requestor ip"
        user == "127.0.0.1;q=0.4" || user == "0:0:0:0:0:0:0:1;q=0.4" | user == "::1;q=0.4"

        and: "Repose will send x-pp-groups with the value IP_Standard"
        ((Handling) sentRequest).request.headers.getFirstValue("x-pp-groups").equalsIgnoreCase("IP_Standard;q=0.4")

    }

    def "when identifying requests by x-forwarded-for"() {

        when: "Request contains x-forwarded-for"
        def messageChain = deproxy.makeRequest([url: url, headers: ["x-forwarded-for": "10.6.51.192"]])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send x-pp-user with a single value"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1

        and: "Repose will send x-pp-user based on x-forwarded-for"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").contains("10.6.51.192;q=0.4")

        and: "Repose will send x-pp-groups with the value IP_Standard"
        ((Handling) sentRequest).request.headers.getFirstValue("x-pp-groups").equalsIgnoreCase("IP_Standard;q=0.4")
    }
}
