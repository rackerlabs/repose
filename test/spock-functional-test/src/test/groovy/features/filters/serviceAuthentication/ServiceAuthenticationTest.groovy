package features.filters.serviceAuthentication

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Response

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
}