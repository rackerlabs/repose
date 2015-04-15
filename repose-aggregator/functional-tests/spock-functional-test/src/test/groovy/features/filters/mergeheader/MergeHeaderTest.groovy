package features.filters.mergeheader

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

/**
 * Created by jennyvo on 4/15/15.
 */
class MergeHeaderTest extends ReposeValveTest {
    static Map params = [:]
    def setupSpec() {
        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/mergeheader", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }
    def "Should not split request headers when configured as such with merge-header filter"() {
        given: "configurged to to split headers using the merge-header filter"

        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders =
                [
                        "user-agent": userAgentValue,
                        "x-pp-user" : "usertest1, usertest2, usertest3",
                        "accept"    : "application/xml;q=1 , application/json;q=0.5",
                        "Accept-Charset" : "UTF-8, US-ASCII, ISO-8859-1, *"
                ]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: reqHeaders)

        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.getHeaders().findAll("user-agent").size() == 1
        mc.handlings[0].request.headers['user-agent'] == userAgentValue
        mc.handlings[0].request.getHeaders().findAll("x-pp-user").size() == 1
        mc.handlings[0].request.getHeaders().findAll("accept").size() == 1
        // split this header since it's not in merge-header config
        mc.handlings[0].request.getHeaders().findAll("Accept-Charset").size() == 4
    }

    def "Should merge response headers when configured as such with merge-header filter"() {
        given: "Origin service returns headers "
        def respHeaders = [
                "aurl": "http://somehost.com/blah, http://somehost.com/bloo",
                "accept": "application/xml",
                "accept": "application/json",
                "Content": "none"]
        def handler = { request -> return new Response(201, "Created", respHeaders, "") }

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', defaultHandler: handler)

        then:
        mc.receivedResponse.code == "201"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.findAll("aurl").size() == 1
        mc.receivedResponse.headers['aurl'].contains("http://somehost.com/bloo")
        mc.receivedResponse.headers.findAll("via").size() == 1
        mc.receivedResponse.headers.findAll("accept").size() == 1
        mc.receivedResponse.headers['accept'].contains("application/json")
    }
}
