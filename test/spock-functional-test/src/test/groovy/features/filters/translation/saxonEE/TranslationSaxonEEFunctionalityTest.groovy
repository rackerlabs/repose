package features.filters.translation.saxonEE

import framework.ReposeValveTest
import framework.category.SaxonEE
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response

@Category(SaxonEE.class)
class TranslationSaxonEEFunctionalityTest extends ReposeValveTest {

    def static String xmlPayLoad = "<a>test</a>"
    def static String jsonPayload = "{\"a\":\"1\",\"b\":\"2\"}"
    def static String jsonInXml = "<entry xml:lang=\"en\" xmlns=\"http://www.w3.org/2005/Atom\">    <category term=\"image.upload\"/>    <category term=\"DATACENTER=ord1\"/>    <category term=\"REGION=preprod-ord\"/>    <content type=\"application/json\"> {  \"event_type\": \"image.upload\",  \"timestamp\": \"2013-04-09 23:18:57.557571\",  \"message_id\": \"b\",  \"payload\": {    \"updated_at\": \"2013-04-09T23:18:57\",    \"id\": \"9\"    } }    </content> </entry>"



    def static Map acceptXML = ["accept": "application/xml"]
    def static Map contentXML = ["content-type": "application/xml"]
    def static Map contentJSON = ["content-type": "application/json"]


    def static String xmlJSON = ["<json:string name=\"field1\">value1</json:string>", "<json:string name=\"field2\">value2</json:string>"]
    def static String remove = "remove-me"
    def static String add = "add-me"

    //Start repose once for this particular translation test
    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        def saxonHome = System.getenv("SAXON_HOME")

        assert saxonHome != null

        repose.addToClassPath(saxonHome)

        repose.applyConfigs(
                "features/filters/translation/common",
                "features/filters/translation/saxonEE"
        )
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanupSpec() {
        if(deproxy)
            deproxy.shutdown()
        if(repose)
            repose.stop()
    }

    def "when translating json within xml in the request body"() {

        given: "Repose is configured to translate request headers"
        def xmlResp = { request -> return new Response(200, "OK", respHeaders) }


        when: "User passes a request through repose"
        def resp = deproxy.makeRequest((String) reposeEndpoint, method, reqHeaders, reqBody, xmlResp)
        def sentRequest = ((MessageChain) resp).getHandlings()[0]

        then: "Request headers sent from repose to the origin service should contain"

        for (String st : bodyShouldContain) {
            assert(((Handling) sentRequest).request.body.contains(st))

        }

        where:
        reqHeaders             | respHeaders | reqBody   | method | bodyShouldContain
        acceptXML + contentXML | contentXML  | jsonInXml | "POST" | ["<category term=\"DATACENTER=req1\"></category>", "<category term=\"REGION=req\"></category>"]

    }

    def "when translating json within xml in the response body"() {

        given: "Origin service returns body of type " + respHeaders
        def xmlResp = { request -> return new Response(200, "OK", respHeaders, respBody) }


        when: "User sends requests through repose"
        def resp = deproxy.makeRequest((String) reposeEndpoint, "PUT", reqHeaders, "something", xmlResp)

        then: "Response body should contain"
        for (String st : shouldContain) {
            assert(resp.receivedResponse.body.contains(st))
        }

        and: "Response code should be"
        resp.receivedResponse.code.equalsIgnoreCase(respCode.toString())

        where:
        reqHeaders | respHeaders | respBody  | respCode | shouldContain
        acceptXML  | contentXML  | jsonInXml | 200      | ["<category term=\"REGION=req\"></category>","<category term=\"DATACENTER=req1\"></category>"]


    }
}
