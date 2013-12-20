package features.filters.serviceAuthentication

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

class ServiceAuthenticationTest extends ReposeValveTest {

    def handler200 = { request -> return new Response(200, "test") }


    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint( properties.getProperty( "target.port" ).toInteger() )

        repose.applyConfigs( "features/filters/serviceAuthentication" )
        repose.start()
    }

    def cleanupSpec() {

        if (deproxy) {

            deproxy.shutdown()
        }

        if (repose) {

            repose.stop()
        }
    }

    def "service authentication populates header to endpoint, repose returns 200"() {

        when:
        def messageChain = deproxy.makeRequest( [ url: reposeEndpoint, defaultHandler: handler200 ] )

        then:
        def authList = messageChain.getHandlings()[ 0 ].getRequest().getHeaders().findAll( "authorization" )
        authList.size == 1
        authList.get( 0 ) == "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="
        messageChain.receivedResponse.code == "200"

    }

    def "when endpoint returns 403, repose returns 500"() {

        when:
        def messageChain = deproxy.makeRequest( [ url: reposeEndpoint, defaultHandler: { request -> return new Response(403, "test") } ] )

        then:
        messageChain.receivedResponse.code == "500"
    }

    def "when endpoint returns 501, repose returns 500"() {

        when:
        def messageChain = deproxy.makeRequest( [ url: reposeEndpoint, defaultHandler: { request -> return new Response(501, "test") } ] )

        then:
        messageChain.receivedResponse.code == "500"
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
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/", method: 'GET', headers: reqHeaders)

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
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/", method: 'GET', defaultHandler: handler)

        then:
        mc.receivedResponse.code == "201"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers['location'] == "http://somehost.com/blah?a=b,c,d"
        mc.receivedResponse.headers.findAll("via").size() == 1
    }
}
