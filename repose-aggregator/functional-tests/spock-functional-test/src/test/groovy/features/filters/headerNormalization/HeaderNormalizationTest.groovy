package features.filters.headerNormalization

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

/**
 * User: dimi5963
 * Date: 9/9/13
 * Time: 10:55 AM
 */
class HeaderNormalizationTest extends ReposeValveTest {

    def headers = [
            'user1':'usertest1',
            'X-Auth-Token':'358484212:99493',
            'X-First-Filter':'firstValue',
            'X-SeCoND-Filter':'secondValue',
            'X-third-filter':'thirdValue',
            'X-last-Filter':'lastValue',
            'X-User-Token':'something'
    ]

    def setupSpec() {
        repose.applyConfigs(
                "features/filters/headerNormalization/"
        )
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getReposeProperty("target.port").toInteger())

    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    def "When Filtering Based on URI and Method" () {
        when:
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: 'GET',
                            url:reposeEndpoint + "/v1/usertest1/servers/something",
                            headers:headers
                    ])

        then:
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == 1
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-auth-token") == '358484212:99493'
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-first-filter") == 'firstValue'
        mc.orphanedHandlings[0].request.headers.findAll("x-second-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-third-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-last-filter") == []
        mc.orphanedHandlings[0].request.headers.getFirstValue("via").contains('1.1 localhost:8888 (Repose/')
        mc.receivedResponse.code == '200'
    }

    def "When Filtering Based on URI"(){
        when:
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: 'POST',
                            url:reposeEndpoint + "/v1/usertest1/servers/something",
                            headers:headers
                    ])

        then:
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == 1
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-auth-token") == '358484212:99493'
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-second-filter") == 'secondValue'
        mc.orphanedHandlings[0].request.headers.findAll("x-first-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-third-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-last-filter") == []
        mc.orphanedHandlings[0].request.headers.getFirstValue("via").contains('1.1 localhost:8888 (Repose/')
        mc.receivedResponse.code == '200'

    }

    def "When Filtering Based on Method"(){
        when:
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: 'POST',
                            url:reposeEndpoint + "/v1/usertest1/resources/something",
                            headers:headers
                    ])
        then:
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == 1
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-auth-token") == '358484212:99493'
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-third-filter") == 'thirdValue'
        mc.orphanedHandlings[0].request.headers.findAll("x-second-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-first-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-last-filter") == []
        mc.orphanedHandlings[0].request.headers.getFirstValue("via").contains('1.1 localhost:8888 (Repose/')
        mc.receivedResponse.code == '200'
    }

    def "When Filtering using catch all"(){
        when:
        MessageChain mc =
            deproxy.makeRequest(
                    [
                            method: 'GET',
                            url:reposeEndpoint + "/v1/usertest1/resources/something",
                            headers:headers
                    ])
        then:
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == 0
        mc.handlings[0].request.headers.findAll("x-auth-token") == []
        mc.handlings[0].request.headers.getFirstValue("x-user-token") == 'something'
        mc.handlings[0].request.headers.getFirstValue("user1") == 'usertest1'
        mc.handlings[0].request.headers.findAll("x-last-filter") == []
        mc.handlings[0].request.headers.getFirstValue("x-second-filter") == 'secondValue'
        mc.handlings[0].request.headers.getFirstValue("x-third-filter") == 'thirdValue'
        mc.handlings[0].request.headers.getFirstValue("x-first-filter") == 'firstValue'
        mc.handlings[0].request.headers.getFirstValue("via").contains('1.1 localhost:8888 (Repose/')
        mc.receivedResponse.code == '200'
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
