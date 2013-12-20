package features.filters.ipidentity

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder
import org.rackspace.deproxy.Response
import spock.lang.Shared

@Category(Slow.class)
class IpIdentityTest extends ReposeValveTest {

    @Shared
    def url
    def setupSpec() {
        int deproxyPort = PortFinder.Singleton.getNextOpenPort()
        int reposePort = PortFinder.Singleton.getNextOpenPort()
        deproxy = new Deproxy()
        deproxy.addEndpoint(deproxyPort)

        url = "http://localhost:${reposePort}"

        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs(
                "common",
                ["reposePort": reposePort.toString(),
                        "targetPort": deproxyPort.toString()]);
        repose.configurationProvider.applyConfigs(
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

    def "Should not split request headers according to rfc"() {
        given:
        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders =
            [
                    "user-agent": userAgentValue,
                    "x-pp-user": "usertest1, usertest2, usertest3",
                    "accept": "application/xml;q=1 , application/json;q=0.5"
            ]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: url, method: 'GET', headers: reqHeaders)

        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.getHeaders().findAll("user-agent").size() == 1
        mc.handlings[0].request.headers['user-agent'] == userAgentValue
        mc.handlings[0].request.getHeaders().findAll("x-pp-user").size() == 4
        mc.handlings[0].request.getHeaders().findAll("accept").size() == 2
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def handler = { request -> return new Response(201, "Created", respHeaders, "") }

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: url, method: 'GET', defaultHandler: handler)

        then:
        mc.receivedResponse.code == "201"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers['location'] == "http://somehost.com/blah?a=b,c,d"
        mc.receivedResponse.headers.findAll("via").size() == 1
    }
}
