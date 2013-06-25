package features.filters.translation

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response

class TranslationHeadersQueriesTest extends ReposeValveTest {

    def static String xmlPayLoad = "<a>test</a>"
    def static String rssPayload = "<a>test body</a>"
    def static String xmlPayloadWithEntities = "<?xml version=\"1.0\" standalone=\"no\" ?> <!DOCTYPE a [   <!ENTITY c SYSTEM  \"/etc/passwd\"> ]>  <a><remove-me>test</remove-me>&quot;somebody&c;</a>"
    def static String jsonPayload = "{\"a\":\"1\",\"b\":\"2\"}"
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
                "features/filters/translation/headersQueries"
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

    def "when translating request headers"() {

        given: "Repose is configured to translate request headers"
        def xmlResp = { request -> return new Response(200, "OK", respHeaders) }


        when: "User passes a request through repose"
        def resp = deproxy.makeRequest((String) reposeEndpoint, method, reqHeaders, reqBody, xmlResp)
        def sentRequest = ((MessageChain) resp).getHandlings()[0]

        then: "Request headers sent from repose to the origin service should contain"

        for (String st : headersShouldContain) {
            ((Handling) sentRequest).request.body.contains(st)

        }

        where:
        reqHeaders             | respHeaders | reqBody    | method | headersShouldContain
        acceptXML + contentXML | contentXML  | xmlPayLoad | "POST" | ["extra-header"]

    }

    def "when translating request query parameters"() {

        given: "Repose is configured to translate request query params"
        def xmlResp = { request -> return new Response(200, "OK", respHeaders) }


        when: "User passes a request through repose"
        def resp = deproxy.makeRequest((String) reposeEndpoint + "/path/to/resource/", method, reqHeaders, reqBody, xmlResp)
        def sentRequest = ((MessageChain) resp).getHandlings()[0]

        then: "Request url sent from repose to the origin service should contain"

        for (String st : queryParamsShouldContain) {
            ((Handling) sentRequest).getRequest().path.contains(st)

        }

        where:
        reqHeaders              | respHeaders | reqBody     | method | queryParamsShouldContain
        acceptXML + contentJSON | contentXML  | jsonPayload | "POST" | ["extra-query=result"]

    }


}