package features.filters.datastore
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

class DistDatastoreFilterTest extends ReposeValveTest {

    String DD_URI
    def DD_HEADERS = ['X-PP-Host-Key':'temp', 'X-TTL':'10']
    def BODY = "test body"
    def KEY

    def setupSpec() {
        repose.applyConfigs("features/filters/datastore/")
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def setup() {
        DD_URI = reposeEndpoint + "/powerapi/dist-datastore/objects/"
        KEY = UUID.randomUUID().toString()
    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    def "PUT a new cache object should return 202 response" () {
        when:
        MessageChain mc = deproxy.makeRequest([method: 'PUT', url:DD_URI + KEY, headers:DD_HEADERS, body: BODY])

        then:
        mc.receivedResponse.code == '202'
    }

    def "PUT a cache object to an existing key should overwrite the cached value"() {

        when: "I make 2 PUT calls for 2 different values for the same key"
        String newBody = "MY NEW VALUE"
        deproxy.makeRequest([method: 'PUT', url:DD_URI + KEY, headers:DD_HEADERS, requestBody: BODY])
        deproxy.makeRequest([method: 'PUT', url:DD_URI + KEY, headers:DD_HEADERS, requestBody: newBody])

        and: "I get the value for the key"
        MessageChain mc = deproxy.makeRequest([method: 'GET', url:DD_URI + KEY, headers:DD_HEADERS])

        then: "The body of the get response should be my second request body"
        mc.receivedResponse.body == newBody
    }

    def "when checking cache object time to live"(){
        given:
        def headers = ['X-PP-Host-Key':'temp', 'X-TTL':'5']
        def objectkey = '8e969a44-990b-de49-d894-cf200b7d4c11'
        def body = "test data"
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: 'PUT',
                            url:reposeEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers,
                            requestBody: body
                    ])
        mc =
            deproxy.makeRequest(
                    [
                            method: 'GET',
                            url:reposeEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers
                    ])
        mc.receivedResponse.code == '200'

        when:
        Thread.sleep(7500)
        mc =
            deproxy.makeRequest(
                    [
                            method: 'GET',
                            url:reposeEndpoint + "/powerapi/dist-datastore/objects/" + objectkey,
                            headers:headers
                    ])

        then:
        mc.receivedResponse.code == '404'

    }

    def "when deleting cache objects"(){
        given:
        def headers = ['X-PP-Host-Key':'temp', 'x-ttl':'1000']
        def objectkey = '8e969a44-990b-de49-d894-cf200b7d4c11'
        def body = "test data"
        def url = reposeEndpoint + "/powerapi/dist-datastore/objects/" + objectkey



        when: "Adding the object to the datastore"
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: "PUT",
                            url:url,
                            headers:headers,
                            requestBody: body
                    ])

        then: "should report success"
        mc.receivedResponse.code == "202"
        mc.receivedResponse.body == ""



        when: "checking that it's there"
        mc =
            deproxy.makeRequest(
                    [
                            method: "GET",
                            url:url,
                            headers:headers
                    ])

        then: "should report that it is"
        mc.receivedResponse.code == "200"
        mc.receivedResponse.body == body



        when: "deleting the object from the datastore"
        mc =
            deproxy.makeRequest(
                    [
                            method: "DELETE",
                            url:url,
                            headers:headers,
//                            body: body
                    ])

        then: "should report that it was successfully deleted"
        mc.receivedResponse.code == "202"
        mc.receivedResponse.body == ""



        when: "checking that it's gone"
        mc =
            deproxy.makeRequest(
                    [
                            method: "GET",
                            url:url,
                            headers:headers,
//                            body: body
                    ])

        then: "should report it missing"
        mc.receivedResponse.code == "404"
        mc.receivedResponse.body == ""
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
