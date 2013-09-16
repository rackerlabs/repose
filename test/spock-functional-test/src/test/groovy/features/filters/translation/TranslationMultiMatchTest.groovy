package features.filters.translation

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response
import spock.lang.Unroll

class TranslationMultiMatchTest extends ReposeValveTest {

    def static String xmlPayLoad = "<a><remove-me>test</remove-me>somebody</a>"
    def static String xmlPayloadWithEntities = "<?xml version=\"1.0\" standalone=\"no\" ?> <!DOCTYPE a [   <!ENTITY c SYSTEM  \"/etc/passwd\"> ]>  <a><remove-me>test</remove-me>&quot;somebody&c;</a>"
    def static String simpleXml = "<a>test body</a>"

    def static String jsonPayload = "{\"field1\": \"value1\", \"field2\": \"value2\"}"

    def static Map acceptXML = ["accept": "application/xml"]
    def static Map contentXML = ["content-type": "application/xml"]
    def static Map contentAtom = ["content-type": "application/atom+xml"]
    def static Map contentOther = ["content-type": "application/other"]
    def static String remove = "remove-me"
    def static String add = "add-me"
    def testHeaders = ['test': 'x', 'other': 'y']

    //Start repose once for this particular translation test
    def setupSpec() {

        repose.applyConfigs(
                "features/filters/translation/common",
                "features/filters/translation/multimatch"
        )
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def setup() {

    }

    def cleanup() {
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    def "when translating responses"() {

        given: "Repose is configured to translate responses using multimatch"
        def xmlResp = { request -> return new Response(200, "OK", respHeaders, respBody) }


        when: "The origin service sends back a response of type " + respHeaders
        def resp = deproxy.makeRequest((String) reposeEndpoint + '/echobody?testparam=x&otherparam=y', "POST", reqHeaders + testHeaders, respBody, xmlResp)

        then: "Response body received should contain"

        for (String st : shouldContain) {
            resp.receivedResponse.body.contains(st)
        }

        and: "Response body received should not contain"
        for (String st : shouldNotContain) {
            !resp.receivedResponse.body.contains(st)
        }

        and: "Response headers should contain"
        for (String st : shouldContainHeaders) {
            resp.receivedResponse.getHeaders().getNames().contains(st)
        }

        and: "Response code returned should be"
        resp.receivedResponse.code.equalsIgnoreCase(respCode.toString())

        where:
        reqHeaders              | respHeaders  | respBody   | respCode | shouldContain            | shouldNotContain | shouldContainHeaders
        acceptXML               | contentXML   | xmlPayLoad | 200      | ["somebody", simpleXml]  | [remove]         | ["translation-response-a", "translation-response-b"]
        acceptXML + contentAtom | contentOther | simpleXml  | 200      | [simpleXml]              | [remove]         | ["translation-response-a", "translation-response-b"]

    }

    def "when translating request headers"() {

        given: "Repose is configured to translate requests using multimatch"
        def xmlResp = { request -> return new Response(200, "OK", respHeaders) }


        when: "The user sends a request of type " + reqHeaders
        def resp = deproxy.makeRequest((String) reposeEndpoint, method, reqHeaders, reqBody, xmlResp)
        def sentRequest = ((MessageChain) resp).getHandlings()[0]

        then: "Request body from repose to the origin service should contain"

        for (String st : shouldContain) {
            ((Handling) sentRequest).request.body.contains(st)

        }

        and: "Request headers sent from repose to the origin service should contain"
        for (String st : shouldContainHeaders) {
            ((Handling) sentRequest).request.getHeaders().getNames().contains(st)
        }

        and: "Request headers sent from repose to the origin service should not contain "
        for (String st : shouldNotContainHeaders) {
            !((Handling) sentRequest).request.getHeaders().getNames().contains(st)
        }

        where:
        reqHeaders              | respHeaders | reqBody   | shouldContain | method | shouldContainHeaders               | shouldNotContainHeaders
        acceptXML + contentXML  | contentXML  | simpleXml | [simpleXml]   | "POST" | ["translation-a", "translation-b"] | []
        acceptXML + contentAtom | contentXML  | simpleXml | [simpleXml]   | "POST" | []                                 | ["translation-a", "translation-b"]


    }

    @Unroll("response: #respHeaders, request: #reqHeaders - #reqBody")
    def "when translating multi-match requests"() {

        given: "Repose is configured to translate requests using multimatch"
        def xmlResp = { request -> return new Response(200, "OK", respHeaders) }



        when: "The user sends a request of type " + reqHeaders
        def resp = deproxy.makeRequest((String) reposeEndpoint + "/somepath?testparam=x&otherparam=y", method, reqHeaders + testHeaders, reqBody, xmlResp)
        def sentRequest = ((MessageChain) resp).getHandlings()[0]

        then: "Request body from repose to the origin service should contain"

        resp.receivedResponse.code == "200"
        sentRequest.request.path == requestPath

        for (String st : shouldContain) {
            ((Handling) sentRequest).request.body.contains(st)

        }

        and: "Request headers sent from repose to the origin service should contain"
        for (String st : shouldContainHeaders) {
            ((Handling) sentRequest).request.getHeaders().getNames().contains(st)
        }

        and: "Request headers sent from repose to the origin service should not contain "
        for (String st : shouldNotContainHeaders) {
            !((Handling) sentRequest).request.getHeaders().getNames().contains(st)
        }

        where:
        reqHeaders              | respHeaders | reqBody   | shouldContain | method | shouldContainHeaders               | shouldNotContainHeaders            | requestPath
        acceptXML + contentAtom | contentXML  | simpleXml | [simpleXml]   | "POST" | []                                 | ["translation-a", "translation-b"] | "/somepath?translation-b=b&testparam=x&otherparam=y"
        acceptXML + contentXML  | contentXML  | simpleXml | [simpleXml]   | "POST" | ["translation-a", "translation-b"] | []                                 | "/somepath?translation-b=b&translation-a=a&testparam=x&otherparam=y"


    }


}