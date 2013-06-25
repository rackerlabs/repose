package features.filters.translation

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response

class TranslationRequestTest extends ReposeValveTest {

    def static String xmlPayLoad = "<a><remove-me>test</remove-me>somebody</a>"
    def static String rssPayload = "<a>test body</a>"
    def static String xmlPayloadWithEntities = "<?xml version=\"1.0\" standalone=\"no\" ?> <!DOCTYPE a [   <!ENTITY c SYSTEM  \"/etc/passwd\"> ]>  <a><remove-me>test</remove-me>&quot;somebody&c;</a>"
    def static String jsonPayload = "{\"field1\": \"value1\", \"field2\": \"value2\"}"
    def static String invalidXml = "<a><remove-me>test</remove-me>somebody"
    def static String invalidJson = "{{'field1': \"value1\", \"field2\": \"value2\"]}"


    def static Map acceptXML = ["accept": "application/xml"]
    def static Map contentXML = ["content-type": "application/xml"]
    def static Map contentJSON = ["content-type": "application/json"]
    def static Map contentXMLHTML = ["content-type": "application/xhtml+xml"]
    def static Map contentOther = ["content-type": "application/other"]
    def static Map contentRss = ["content-type": "application/rss+xml"]

    def static String xmlJSON = ["<json:string name=\"field1\">value1</json:string>", "<json:string name=\"field2\">value2</json:string>"]
    def static String remove = "remove-me"
    def static String add = "add-me"

    //Start repose once for this particular translation test
    def setupSpec() {

        repose.applyConfigs(
                "features/filters/translation/common",
                "features/filters/translation/request"
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

    def "when translating requests"() {

        given: "Repose is configured to translate requests"
        def xmlResp = { request -> return new Response(200, "OK", respHeaders) }


        when: "User passes a request through repose"
        def resp = deproxy.makeRequest((String) reposeEndpoint, method, reqHeaders, reqBody, xmlResp)
        def sentRequest = ((MessageChain) resp).getHandlings()[0]

        then: "Request body sent from repose to the origin service should contain"

        for (String st : shouldContain) {
            ((Handling) sentRequest).request.body.contains(st)

        }

        and: "Request body sent from repose to the origin service should not contain"

        for (String st : shouldNotContain) {
            !((Handling) sentRequest).request.body.contains(st)
        }

        where:
        reqHeaders                 | respHeaders | reqBody                | shouldContain  | shouldNotContain | method
        acceptXML + contentXML     | contentXML  | xmlPayLoad             | ["somebody"]   | [remove]         | "POST"
        acceptXML + contentXML     | contentXML  | xmlPayloadWithEntities | ["\"somebody"] | [remove]         | "POST"
        acceptXML + contentXMLHTML | contentXML  | xmlPayLoad             | [add]          | []               | "POST"
        acceptXML + contentXMLHTML | contentXML  | xmlPayLoad             | [xmlPayLoad]   | [add]            | "PUT"
        acceptXML + contentJSON    | contentXML  | jsonPayload            | [add, xmlJSON] | []               | "POST"
        acceptXML + contentOther   | contentXML  | jsonPayload            | [jsonPayload]  | [add]            | "POST"


    }


    def "when translating application/rss+xml requests with header translations"() {

        given: "Repose is configured to translate request headers"
        def respHeaders = ["content-type": "application/xml"]
        def xmlResp = { request -> return new Response(200, "OK", respHeaders, rssPayload) }


        when: "User sends a request through repose"
        def resp = deproxy.makeRequest((String) reposeEndpoint, "POST", contentRss + acceptXML, rssPayload, xmlResp)
        def sentRequest = ((MessageChain) resp).getHandlings()[0]

        then: "Request body sent from repose to the origin service should contain"
        ((Handling) sentRequest).request.body.contains(rssPayload)

        and: "Request headerssent from repose to the origin service should contain"
        ((Handling) sentRequest).request.headers.getNames().contains("translation-header")

    }

    def "when attempting to translate an invalid xml/json request"() {

        given: "Repose is configured to translate requests"
        def xmlResp = { request -> return new Response(200, "OK", respHeaders, invalidXml) }


        when: "User passes invalid json/xml through repose"
        def resp = deproxy.makeRequest((String) reposeEndpoint, "PUT", reqHeaders, "something", xmlResp)

        then: "Repose will send back 400s as the requests are invalid"
        resp.receivedResponse.code.equals(respCode)

        where:
        reqHeaders              | respHeaders | respBody    | respCode | shouldContain | shouldNotContain
        acceptXML + contentJSON | contentXML  | invalidXml  | "400"    | []            | []
        acceptXML + contentJSON | contentXML  | invalidJson | "400"    | []            | []


    }


}