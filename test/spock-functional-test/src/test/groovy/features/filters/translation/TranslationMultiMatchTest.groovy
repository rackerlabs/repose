package features.filters.translation

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response

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

    //Start repose once for this particular translation test
    def setupSpec() {

        repose.applyConfigs(
                "features/filters/translation/common",
                "features/filters/translation/multimatch"
        )
        repose.start()
    }

    def setup() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanup() {
        deproxy.shutdown()
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    def "when translating responses"() {

        given: "Repose is configured to translate responses using multimatch"
        def xmlResp = { request -> return new Response(200, "OK", respHeaders, respBody) }


        when: "The origin service sends back a response of type " + respHeaders
        def resp = deproxy.makeRequest((String) reposeEndpoint, "POST", reqHeaders, "something", xmlResp)

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
        reqHeaders              | respHeaders  | respBody   | respCode | shouldContain | shouldNotContain | shouldContainHeaders
        acceptXML               | contentXML   | xmlPayLoad | 200      | ["somebody"]  | [remove]         | ["translation-response-a", "translation-response-b"]
        acceptXML + contentAtom | contentOther | simpleXml  | 200      | [simpleXml]   | [remove]         | ["translation-response-a", "translation-response-b"]

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
        for (String st : shouldNotContainHeaders) {
            !((Handling) sentRequest).request.getHeaders().contains(st)
        }

        where:
        reqHeaders              | respHeaders | reqBody   | shouldContain | method | shouldContainHeaders               | shouldNotContainHeaders
        acceptXML + contentXML  | contentXML  | simpleXml | [simpleXml]   | "POST" | ["translation-a", "translation-b"] | []
        acceptXML + contentAtom | contentXML  | simpleXml | [simpleXml]   | "POST" | []                                 | ["translation-a", "translation-b"]


    }


}