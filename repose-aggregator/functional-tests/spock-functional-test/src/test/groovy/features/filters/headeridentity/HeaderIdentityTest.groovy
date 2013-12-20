package features.filters.headeridentity

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

class HeaderIdentityTest extends ReposeValveTest {

    def static String user1 = "reposeuser1"
    def static String user2 = "reposeuser2"
    def static Map headersIdSingle = ["x-header-user":user1,"some-other-header":"value"]
    def static Map headersIdMulti = ["x-header-user":user2+","+user1]

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigsRuntime("common", params)
        repose.configurationProvider.applyConfigsRuntime("features/filters/headeridentity", params)
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

    @Unroll("When mapping header: #incomingHeaders to x-pp-user Should use first user in the list: #expecteduser")
    def "when identifying requests by header"() {

        when: "Request contains value(s) of the target header"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, headers: incomingHeaders])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send x-pp-user with a single value"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").size() == 1

        and: "Repose will send x-pp-user based on incoming identity header"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-user").contains(expecteduser+";q=0.2")

        and: "Repose will send x-pp-groups with the value of the targeted identity header"
        ((Handling) sentRequest).request.getHeaders().findAll("x-pp-groups").contains("X-Header-User"+";q=0.2")


        where:
        incomingHeaders | expecteduser
        headersIdSingle | user1
        headersIdMulti  | user2
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
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: reqHeaders)

        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.getHeaders().findAll("user-agent").size() == 1
        mc.handlings[0].request.headers['user-agent'] == userAgentValue
        mc.handlings[0].request.getHeaders().findAll("x-pp-user").size() == 3
        mc.handlings[0].request.getHeaders().findAll("accept").size() == 2
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def handler = { request -> return new Response(201, "Created", respHeaders, "") }

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', defaultHandler: handler)
        def handling = mc.getHandlings()[0]

        then:
        mc.receivedResponse.code == "201"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers['location'] == "http://somehost.com/blah?a=b,c,d"
        mc.receivedResponse.headers.findAll("via").size() == 1
    }
}
