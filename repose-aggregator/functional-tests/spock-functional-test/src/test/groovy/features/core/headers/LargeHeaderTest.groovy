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
    //test failed with headersive > 7.5 KiB ~ 7935 characters
    @Unroll ("#headerName, #headersize")
    def "Repose send req with large header" () {
        given:
        def largeheader = RandomStringUtils.random(headersize, ('A'..'Z').join().toCharArray())

        when: "make a request with the given header and value"
        def headers = [
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

    //test failed with headersive > 7.5 KiB ~ 7935 characters
    @Unroll("Response: #headerName, #headersize")
    def "Repose post req with large header get response" () {
        given:
        def largeheader = RandomStringUtils.random(headersize, ('A'..'Z').join().toCharArray())

        when: "make a request with the given header and value"
        def headers = [
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

}
