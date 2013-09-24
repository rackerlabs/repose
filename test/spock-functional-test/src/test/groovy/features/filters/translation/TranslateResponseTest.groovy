package features.filters.translation

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Response
import org.rackspace.gdeproxy.HeaderCollection
import spock.lang.Unroll

class TranslateResponseTest extends ReposeValveTest {

    def static String xmlResponse = "<a><remove-me>test</remove-me>somebody</a>"
    def static String xmlRssResponse = "<a>test body</a>"
    def static String invalidXmlResponse = "<a><remove-me>test</remove-me>somebody"
    def static String invalidJsonResponse = "{{'field1': \"value1\", \"field2\": \"value2\"]}"
    def static String xmlResponseWithEntities = "<?xml version=\"1.0\" standalone=\"no\" ?> <!DOCTYPE a [   <!ENTITY c SYSTEM  \"/etc/passwd\"> ]>  <a><remove-me>test</remove-me>&quot;somebody&c;</a>"
    def static String xmlResponseWithExtEntities = "<?xml version=\"1.0\" standalone=\"no\" ?> <!DOCTYPE a [  <!ENTITY license_agreement SYSTEM \"http://www.mydomain.com/license.xml\"> ]>  <a><remove-me>test</remove-me>&quot;somebody&license_agreement;</a>"
    def static String xmlResponseWithXmlBomb = "<?xml version=\"1.0\"?> <!DOCTYPE lolz [   <!ENTITY lol \"lol\">   <!ENTITY lol2 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">   <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">   <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">   <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">   <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">   <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">   <!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">   <!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\"> ]> <lolz>&lol9;</lolz>"
    def static String jsonResponse = "{\"field1\": \"value1\", \"field2\": \"value2\"}"

    def static Map acceptXML = ["accept": "application/xml"]
    def static Map acceptJSON = ["accept": "application/json"]

    def static Map contentXML = ["content-type": "application/xml"]
    def static Map contentJSON = ["content-type": "application/json"]
    def static Map contentXMLHTML = ["content-type": "application/xhtml+xml"]
    def static Map contentOther = ["content-type": "application/other"]
    def static Map acceptRss = ["accept": "application/rss+xml"]
    def static ArrayList<String> xmlJSON = ["<json:string name=\"field1\">value1</json:string>", "<json:string name=\"field2\">value2</json:string>"]
    def static String filterChainUnavailable = "filter list not available"
    def static String remove = "remove-me"
    def static String add = "add-me"


    def String convertStreamToString(byte[] input){
        return new Scanner(new ByteArrayInputStream(input)).useDelimiter("\\A").next();
    }

    //Start repose once for this particular translation test
    def setupSpec() {

        repose.applyConfigs(
                "features/filters/translation/common",
                "features/filters/translation/response"
        )
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    @Unroll("when translating responses - request: xml, response: #respHeaders - #respBody")
    def "when translating responses"() {

        given: "Origin service returns body of type " + respHeaders
        def xmlResp = { request -> return new Response(200, "OK", respHeaders, respBody) }


        when: "User sends requests through repose"
        def resp = deproxy.makeRequest((String) reposeEndpoint, method, acceptXML, "something", xmlResp)

        then: "Response body should contain"
        for (String st : shouldContain) {
            if(resp.receivedResponse.body instanceof byte[])
                assert(convertStreamToString(resp.receivedResponse.body).contains(st))
            else
                assert(resp.receivedResponse.body.contains(st))
        }

        and: "Response body should not contain"
        for (String st : shouldNotContain) {
            assert(!resp.receivedResponse.body.contains(st))
        }

        and: "Response code should be"
        resp.receivedResponse.code.equalsIgnoreCase(respCode.toString())

        where:
        respHeaders    | respBody                   | respCode | shouldContain  | shouldNotContain         | method
        contentXML     | xmlResponse                | 200      | ["somebody"]   | [remove]                 | "PUT"
        contentXML     | xmlResponseWithEntities    | 200      | ["\"somebody"] | [remove]                 | "PUT"
        contentXML     | xmlResponseWithXmlBomb     | 500      | []             | [remove]                 | "PUT"
        contentXMLHTML | xmlResponse                | 200      | [add]          | [filterChainUnavailable] | "PUT"
        contentJSON    | jsonResponse               | 200      | xmlJSON + [add]| [filterChainUnavailable] | "PUT"
        contentOther   | jsonResponse               | 200      | [jsonResponse] | [add]                    | "PUT"
        contentXML     | xmlResponseWithExtEntities | 200      | ["\"somebody"] | [remove]                 | "POST"


    }


    def "when translating application/rss+xml response with header translations"() {

        given: "Repose is configured to translate response headers"
        def reqHeaders = ["accept": "application/xml"]
        def respHeaders = ["content-type": "application/rss+xml"]
        def xmlResp = { request -> return new Response(200, "OK", respHeaders, xmlRssResponse) }


        when: "User sends a request through repose"
        def resp = deproxy.makeRequest((String) reposeEndpoint, "PUT", reqHeaders, "something", xmlResp)

        then: "Response body should not be touched"
        resp.receivedResponse.body.contains(xmlRssResponse)
        !resp.receivedResponse.body.contains("add-me")
        resp.receivedResponse.code == "200"

        and: "Response headers should contain added header from translation"
        resp.receivedResponse.getHeaders().names.contains("translation-header")
        !resp.receivedResponse.getHeaders().names.contains("x-powered-by")
    }

    def "when attempting to translate an invalid xml/json response"() {

        given: "Origin service returns invalid json/xml"
        def xmlResp = { request -> return new Response(200, "OK", respHeaders, respBody) }


        when: "User sends a request through repose"
        def resp = deproxy.makeRequest((String) reposeEndpoint, "PUT", reqHeaders, "something", xmlResp)

        then: "Repose should return a 500 as the response is invalid"
        resp.receivedResponse.code.equals(respCode)

        where:
        reqHeaders | respHeaders | respBody            | respCode
        acceptXML  | contentXML  | invalidXmlResponse  | "500"
        acceptXML  | contentJSON | invalidJsonResponse | "500"

    }


}
