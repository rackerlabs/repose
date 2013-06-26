package features.filters.translation

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Response
import org.rackspace.gdeproxy.HeaderCollection

class TranslateResponseTest extends ReposeValveTest {

    def static String xmlResponse = "<a><remove-me>test</remove-me>somebody</a>"
    def static String xmlRssResponse = "<a>test body</a>"
    def static String invalidXmlResponse = "<a><remove-me>test</remove-me>somebody"
    def static String invalidJsonResponse = "{{'field1': \"value1\", \"field2\": \"value2\"]}"
    def static String xmlResponseWithEntities = "<?xml version=\"1.0\" standalone=\"no\" ?> <!DOCTYPE a [   <!ENTITY c SYSTEM  \"/etc/passwd\"> ]>  <a><remove-me>test</remove-me>&quot;somebody&c;</a>"
    def static String jsonResponse = "{\"field1\": \"value1\", \"field2\": \"value2\"}"

    def static Map acceptXML = ["accept": "application/xml"]
    def static Map acceptJSON = ["accept": "application/json"]

    def static Map contentXML = ["content-type": "application/xml"]
    def static Map contentJSON = ["content-type": "application/json"]
    def static Map contentXMLHTML = ["content-type": "application/xhtml+xml"]
    def static Map contentOther = ["content-type": "application/other"]
    def static Map acceptRss = ["accept": "application/rss+xml"]
    def static String xmlJSON = ["<json:string name=\"field1\">value1</json:string>", "<json:string name=\"field2\">value2</json:string>"]
    def static String filterChainUnavailable = "filter list not available"
    def static String remove = "remove-me"
    def static String add = "add-me"

    //Start repose once for this particular translation test
    def setupSpec() {

        repose.applyConfigs(
                "features/filters/translation/common",
                "features/filters/translation/response"
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

        given:
        def xmlResp = { request -> return new Response(200, "OK", respHeaders, respBody) }


        when:
        def resp = deproxy.makeRequest((String) reposeEndpoint, "PUT", reqHeaders, "something", xmlResp)

        then:
        for (String st : shouldContain) {
            resp.receivedResponse.body.contains(st)
        }
        for (String st : shouldNotContain) {
            !resp.receivedResponse.body.contains(st)
        }
        resp.receivedResponse.code.equalsIgnoreCase(respCode.toString())

        where:
        reqHeaders | respHeaders    | respBody                | respCode | shouldContain  | shouldNotContain
        acceptXML  | contentXML     | xmlResponse             | 200      | ["somebody"]   | [remove]
        acceptXML  | contentXML     | xmlResponseWithEntities | 200      | ["\"somebody"] | [remove]
        acceptXML  | contentXMLHTML | xmlResponse             | 200      | [add]          | [filterChainUnavailable]
        acceptXML  | contentJSON    | jsonResponse            | 200      | [xmlJSON, add] | [filterChainUnavailable]
        acceptXML  | contentOther   | jsonResponse            | 200      | [jsonResponse] | [add]


    }


    def "when translating application/rss+xml response with header translations"() {

        given:
        def reqHeaders = ["accept": "application/rss+xml"]
        def respHeaders = ["content-type": "application/xml"]
        def xmlResp = { request -> return new Response(200, "OK", respHeaders, xmlRssResponse) }


        when:
        def resp = deproxy.makeRequest((String) reposeEndpoint, "PUT", reqHeaders, "something", xmlResp)

        then:
        resp.receivedResponse.body.contains(xmlRssResponse)
        !resp.receivedResponse.getHeaders().names.contains("translation-header")

    }

    def "when attempting to translate an invalid xml/json response"() {

        given:
        def xmlResp = { request -> return new Response(200, "OK", respHeaders, respBody) }


        when:
        def resp = deproxy.makeRequest((String) reposeEndpoint, "PUT", reqHeaders, "something", xmlResp)

        then:
        resp.receivedResponse.code.equals(respCode)

        where:
        reqHeaders | respHeaders | respBody            | respCode | shouldContain | shouldNotContain
        acceptXML  | contentXML  | invalidXmlResponse  | "500"    | []            | []
        acceptXML  | contentJSON | invalidJsonResponse | "500"    | []            | []

    }


}