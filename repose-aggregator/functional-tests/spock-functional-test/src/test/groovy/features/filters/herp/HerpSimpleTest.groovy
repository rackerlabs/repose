package features.filters.herp
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
/**
 * Created by jennyvo on 12/16/14.
 */
class HerpSimpleTest extends ReposeValveTest{

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/herp", params)
        repose.start(waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def "Happy path using herp with simple request" () {
        setup: "declare messageChain to be of type MessageChain"
        MessageChain mc
        def Map<String, String> headers = [
                'Accept': 'application/xml',
                'Host'  : 'LocalHost',
                'User-agent': 'gdeproxy'
        ]
        def customHandler = {return new Response(404, "Resource Not Fount", [], reqBody)}

        when: "When Requesting " + method + " " + request
        mc = deproxy.makeRequest(url: reposeEndpoint +
                request, method: method, headers: headers,
                requestBody: reqBody, defaultHandler: customHandler,
                addDefaultHeaders: false
        )

        then: "result should be " + responseCode
        mc.receivedResponse.code.equals(responseCode)

        where:
        responseCode | request                                                | method | reqBody
        "404"        | "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"   | "GET"  | ""
        "404"        | "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"   | "GET"  | ""
        "405"        | "/resource1/id"                                        | "POST" | ""
        "415"        | "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"   | "PUT"  | "some data"

    }
}
