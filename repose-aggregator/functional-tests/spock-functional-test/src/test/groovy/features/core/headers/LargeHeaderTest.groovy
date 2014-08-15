package features.core.headers

import framework.ReposeValveTest
import org.apache.commons.lang.RandomStringUtils
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by jennyvo on 8/13/14.
 * This the boundary test checking the limit size for request headers
 */
class LargeHeaderTest extends ReposeValveTest {

    static int originServicePort
    static int reposePort
    static String url

    def setupSpec() {
        deproxy = new Deproxy()
        originServicePort = properties.targetPort
        deproxy.addEndpoint(originServicePort)
        reposePort = properties.reposePort
        url = "http://localhost:${reposePort}"
        def params = properties.defaultTemplateParams
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/headers", params)

        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)

        repose.waitForNon500FromUrl(url)
    }

    def cleanupSpec() {
        if (repose) {
            repose.stop()
        }
        if (deproxy) {
            deproxy.shutdown()
        }
    }
    //test failed with total headers size > 8192 Characters
    @Unroll ("#headerName, #headersize")
    def "Repose send req with large header" () {
        given:
        def largeheader = RandomStringUtils.random(headersize, ('A'..'Z').join().toCharArray())
        def tokengen = RandomStringUtils.random(740, ('A'..'Z').join().toCharArray())

        when: "make a request with the given header and value"
        def headers = [
                'X-Auth-Token'  : tokengen.toString(),
                'Content-Length': '0'
        ]

        headers[headerName.toString()] = largeheader.toString()

        MessageChain mc = deproxy.makeRequest(url: url, headers: headers)

        then: "the request should"
        mc.handlings.size() == 1
        mc.receivedResponse.code == "200"
        mc.handlings[0].request.headers.contains(headerName)
        mc.handlings[0].request.headers.getFirstValue(headerName) == largeheader

        where:
        [headerName, headersize] <<
            [["Warning", "WWW-Authenticate"], [1024, 4096, 6144, 7168]].combinations()
    }


    @Unroll("Response: #headerName, #headersize")
    def "Repose post req with large header get response" () {//test failed with total headers size > 8192 Characters
        given:
        def largeheader = RandomStringUtils.random(headersize, ('A'..'Z').join().toCharArray())
        def tokengen = RandomStringUtils.random(740, ('A'..'Z').join().toCharArray())

        when: "make a request with the given header and value"
        def headers = [
                'X-Auth-Token'  : tokengen.toString(),
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = largeheader.toString()

        MessageChain mc = deproxy.makeRequest(url: url, defaultHandler: { new Response(200, null, headers) })

        then: "the response should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.receivedResponse.code == "200"
        mc.receivedResponse.headers.contains(headerName)
        mc.receivedResponse.headers.getFirstValue(headerName) == largeheader

        where:
        [headerName, headersize] <<
                [["Warning", "WWW-Authenticate"], [1024, 4096, 6144, 7168]].combinations()
    }

    //with total headers size > 8192 Characters repose should not handle and resp 413
    def "Repose send req with total headers size > 8192 should not handle" () {
        given:
        def largeheader = RandomStringUtils.random(8100, ('A'..'Z').join().toCharArray())

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]

        headers["WWW-Authenticate"] = largeheader.toString()

        MessageChain mc = deproxy.makeRequest(url: url, headers: headers)

        then: "the request should"
        mc.handlings.size() == 0
        mc.receivedResponse.code == "413"
    }

    //with total headers size > 8192, orgin service resp 500
    def "Repose send req with total headers size > 8192 should resp 500" () {
        given:
        def largeheader = RandomStringUtils.random(8100, ('A'..'Z').join().toCharArray())

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]

        headers["WWW-Authenticate"] = largeheader.toString()

        MessageChain mc = deproxy.makeRequest(url: url, defaultHandler: { new Response(200, null, headers) })

        then: "the request should not handle resp"
        mc.receivedResponse.code == "500"
    }
}
