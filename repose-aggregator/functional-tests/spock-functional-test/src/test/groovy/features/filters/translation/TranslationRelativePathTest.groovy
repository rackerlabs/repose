package features.filters.translation

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by jennyvo on 7/29/14.
 */
class TranslationRelativePathTest extends ReposeValveTest{
    def static Map acceptXML = ["accept": "application/xml"]
    def static Map contentXML = ["content-type": "application/xml"]
    def static String xmlPayLoad = "<a><remove-me>test</remove-me>somebody</a>"
    def static String xmlPayloadWithEntities = "<?xml version=\"1.0\" standalone=\"no\" ?> <!DOCTYPE a [   <!ENTITY c SYSTEM  \"/etc/passwd\"> ]>  <a><remove-me>test</remove-me>&quot;somebody&c;</a>"
    def static String xmlPayloadWithXmlBomb = "<?xml version=\"1.0\"?> <!DOCTYPE lolz [   <!ENTITY lol \"lol\">   <!ENTITY lol2 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">   <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">   <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">   <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">   <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">   <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">   <!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">   <!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\"> ]> <lolz>&lol9;</lolz>"
    def static String remove = "remove-me"
    //Start repose once for this particular translation test
    def setupSpec() {

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/relativepath", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()
        if (repose)
            repose.stop()
    }

    def String convertStreamToString(byte[] input){
        return new Scanner(new ByteArrayInputStream(input)).useDelimiter("\\A").next();
    }

    @Unroll("response: xml, request: #reqHeaders - #reqBody")
    def "when translating requests using .xsl from relative path"() {

        given: "Repose is configured to translate requests"
        def xmlResp = { request -> return new Response(200, "OK", contentXML) }


        when: "User passes a request through repose"
        def resp = deproxy.makeRequest(url: (String) reposeEndpoint, method: method, headers: reqHeaders, requestBody: reqBody, defaultHandler: xmlResp)
        def sentRequest = ((MessageChain) resp).handlings[0]

        then: "Request body sent from repose to the origin service should contain"

        resp.receivedResponse.code == responseCode

        if (responseCode != "400") {
            for (String st : shouldContain) {
                if (sentRequest.request.body instanceof byte[])
                    assert (convertStreamToString(sentRequest.request.body).contains(st))
                else
                    assert (sentRequest.request.body.contains(st))
            }
        }


        and: "Request body sent from repose to the origin service should not contain"

        if (responseCode != "400") {
            for (String st : shouldNotContain) {
                if (sentRequest.request.body instanceof byte[])
                    assert (!convertStreamToString(sentRequest.request.body).contains(st))
                else
                    assert (!sentRequest.request.body.contains(st))
            }
        }

        where:
        reqHeaders             | reqBody                | shouldContain  | shouldNotContain | method | responseCode
        acceptXML + contentXML | xmlPayLoad             | ["somebody"]   | [remove]         | "POST" | '200'
        acceptXML + contentXML | xmlPayloadWithEntities | ["\"somebody"] | [remove]         | "POST" | '200'
        acceptXML + contentXML | xmlPayloadWithXmlBomb  | ["\"somebody"] | [remove]         | "POST" | '400'

    }
}