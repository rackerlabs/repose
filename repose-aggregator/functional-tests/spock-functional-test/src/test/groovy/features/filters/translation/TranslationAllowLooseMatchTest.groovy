package features.filters.translation

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by jennyvo on 5/2/14.
 */
class TranslationAllowLooseMatchTest extends ReposeValveTest {

    def static String xmlPayLoad = "<a><remove-me>test</remove-me>somebody</a>"
    def static String xmlPayloadWithEntities = "<?xml version=\"1.0\" standalone=\"no\" ?> <!DOCTYPE a [   <!ENTITY c SYSTEM  \"/etc/passwd\"> ]>  <a><remove-me>test</remove-me>&quot;somebody&c;</a>"
    def static String simpleXml = "<a>test body</a>"

    def static String jsonPayload = "{\"field1\": \"value1\", \"field2\": \"value2\"}"

    def static Map acceptXML = ["accept": "application/xml"]
    def static Map acceptOther = ["accept": "application/other"]
    def static Map contentXML = ["content-type": "application/xml"]
    def static Map contentAtom = ["content-type": "application/atom+xml"]
    def static Map contentOther = ["content-type": "application/other"]
    def static String remove = "remove-me"
    def static String add = "add-me"
    def testHeaders = ['test': 'x', 'other': 'y']


    def String convertStreamToString(byte[] input){
        return new Scanner(new ByteArrayInputStream(input)).useDelimiter("\\A").next();
    }

    //Start repose once for this particular translation test
    def setupSpec() {

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/allowloosematch", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }
    @Unroll("When req with content-type: #contenttype")
    def "Allow looser matches on Content-type configuration setting"() {
        given:
        def headers =
                [
                        "content-type" : contenttype ,
                        "accept"    : "application/xml"
                ]
        def handler = { request -> return new Response(201, "Created", headers, xmlPayLoad) }

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/somepath?testparam=x&otherparam=y", method: 'POST', headers: headers, requestBody:xmlPayLoad, defaultHandler: handler)

        then:
        mc.handlings.size() == 1
        mc.receivedResponse.code == "201"
        mc.receivedResponse.body == response_to_client
        mc.handlings[0].response.body == response_from_origin
        mc.receivedResponse.getHeaders().findAll("Content-Type").size() == 1
        !mc.receivedResponse.body.toString().contains("httpx:unknown-content")

        where:
        contenttype                        | response_from_origin | response_to_client
        "application/atom+xml"             | xmlPayLoad           | "<a>somebody</a>"
        "application/atom+xml; type=event" | xmlPayLoad           | "<a>somebody</a>"
        "application/atom+xml; v=1"        | xmlPayLoad           | "<a>somebody</a>"
        "application/atom"                 | xmlPayLoad           | xmlPayLoad
        "application/xml; v=1"             | xmlPayLoad           | xmlPayLoad
        "text/plain; v=1"                  | xmlPayLoad           | xmlPayLoad
        "foo/a"                            | xmlPayLoad           | xmlPayLoad
        "foo/x;"                           | xmlPayLoad           | xmlPayLoad
        "foo/x;foo=bar,bar=foo,type=foo"   | xmlPayLoad           | xmlPayLoad
        "foo=bar;foo/x"                    | xmlPayLoad           | xmlPayLoad
        "foo/x;foo=bar,text/plain;v=1.1"   | xmlPayLoad           | xmlPayLoad
    }
}
