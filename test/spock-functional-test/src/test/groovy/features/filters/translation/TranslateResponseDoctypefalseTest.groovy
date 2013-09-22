package features.filters.translation

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Response
import spock.lang.Unroll

class TranslateResponseDoctypefalseTest extends ReposeValveTest {

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
    def static String xmlJSON = ["<json:string name=\"field1\">value1</json:string>", "<json:string name=\"field2\">value2</json:string>"]
    def static String filterChainUnavailable = "filter list not available"
    def static String remove = "remove-me"
    def static String add = "add-me"

    //Start repose once for this particular translation test
    def setupSpec() {

        repose.applyConfigs(
                "features/filters/translation/responsedocfalse"
        )
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    @Unroll("response: #respBody")
    def "when translating xml responses with doctype set to failse"() {

        given: "Origin service returns body of type " + contentXML
        def xmlResp = { request -> return new Response(200, "OK", contentXML, respBody) }


        when: "User sends requests through repose"
        def resp = deproxy.makeRequest((String) reposeEndpoint + "/translation/responsedocfalse/123", "POST", acceptXML, "something", xmlResp)

        then: "Response code should be"
        resp.receivedResponse.code == "500"

        where:
        respBody << [
                xmlResponseWithEntities,
                xmlResponseWithExtEntities
        ]

    }



}
