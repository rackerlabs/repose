package features.filters.datastore

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

class DistDatastoreFilterGetTest extends ReposeValveTest {

    def DD_URI
    def DD_HEADERS = ['X-PP-Host-Key':'temp', 'X-TTL':'10']
    def KEY
    def DD_PATH = "/powerapi/dist-datastore/objects/"

    def setupSpec() {
        repose.applyConfigs("features/filters/datastore/")
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def setup() {
        DD_URI = reposeEndpoint + DD_PATH
        KEY = UUID.randomUUID().toString()
    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    def "GET with no key should return 400 BAD REQUEST"() {
        when:
        MessageChain mc = deproxy.makeRequest([method: 'GET', url:DD_URI, headers:DD_HEADERS])

        then:
        mc.receivedResponse.code == '400'
    }

    def "GET with missing X-PP-Host-Key should return a 400 BAD REQUEST"() {

        when:
        MessageChain mc = deproxy.makeRequest([method: 'GET', url:DD_URI + KEY])

        then:
        mc.receivedResponse.code == '400'
    }

    def "GET of invalid key should fail with 400"() {

        given:
        def badKey = "////////" + KEY

        when: "I attempt to get the value from cache"
        MessageChain mc = deproxy.makeRequest([method: 'GET', url:reposeEndpoint, path:DD_PATH + badKey, headers:DD_HEADERS])

        then:
        mc.receivedResponse.code == '400'
    }

    def "GET of key after time to live has expired should return a 404"(){

        given:
        MessageChain mc = deproxy.makeRequest([method: 'PUT', url:DD_URI + KEY, headers:['X-PP-Host-Key':'temp', 'X-TTL':'2'], requestBody: "foo"])

        when:
        mc = deproxy.makeRequest([method: 'GET', url:DD_URI + KEY, headers:DD_HEADERS])

        then:
        mc.receivedResponse.code == '200'
        mc.receivedResponse.body == "foo"

        when: "I wait long enough for the TTL of the item to expire"
        Thread.sleep(3000)

        and: "I get the key again"
        mc = deproxy.makeRequest([method: 'GET', url:DD_URI + KEY, headers:DD_HEADERS])

        then:
        mc.receivedResponse.code == '404'
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
