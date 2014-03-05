package features.filters.translation

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * User: dimi5963
 * Date: 3/5/14
 * Time: 9:49 AM
 */
class TranslateUncommonHeadersTest extends ReposeValveTest {
    def static Map acceptXML = ["accept": "application/xml"]
    def static Map acceptJSON = ["accept": "application/json"]
    def static Map contentTypeAtomXML = ['content-type': 'application/atom+xml']
    def static Map contentTypeXML = ['content-type': 'application/xml']
    def static Map contentTypeJSON = ['content-type': 'application/json']
    def static Map weirdContentTypeAtomXML = ['content-type': 'application/atom+xml; type=entry;charset=utf-8']
    def static Map weirdAcceptAtomXML = ['accept': 'application/atom+xml; type=entry;charset=utf-8']
    def static Map weirdContentTypeXML = ['content-type': 'application/xml; type=entry;charset=utf-8']
    def static Map weirdAcceptXML = ['accept': 'application/xml; type=entry;charset=utf-8']
    def static Map weirdContentTypeJSON = ['content-type': 'application/json; type=entry;charset=utf-8']
    def static Map weirdAcceptJSON = ['accept': 'application/json; type=entry;charset=utf-8']
    def static respXMLBody = '<atom:entry xmlns:atom="http://www.w3.org/2005/Atom" xmlns="http://wadl.dev.java.net/2009/02" xmlns:db="http://docbook.org/ns/docbook" xmlns:error="http://docs.rackspace.com/core/error" xmlns:wadl="http://wadl.dev.java.net/2009/02" xmlns:json="http://json-schema.org/schema#" xmlns:d317e1="http://wadl.dev.java.net/2009/02" xmlns:usage-summary="http://docs.rackspace.com/core/usage-summary">\n' +
            '  <atom:title type="text">Slice Action</atom:title>\n' +
            '  <atom:author>\n' +
            '    <atom:name>Name</atom:name>\n' +
            '  </atom:author>\n' +
            '  <atom:content type="application/xml">\n' +
            '    <event xmlns="http://docs.rackspace.com/core/event" xmlns:csd="http://docs.rackspace.com/event/servers/slice">\n' +
            '      <csd:product huddleId="202" rootPassword="12345678" version="1">\n' +
            '        <csd:sliceMetaData key="key1" value="value1"/>\n' +
            '        <csd:additionalPublicAddress dns1="1.1.1.1" dns2="1.1.1.1" ip="1.1.1.1"/>\n' +
            '      </csd:product>\n' +
            '    </event>\n' +
            '  </atom:content>\n' +
            '</atom:entry>'

    def static respJSONBody = '{"id": "remove-me"}'

    //Start repose once for this particular translation test
    def setupSpec() {

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/uncommon", params)
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    @Unroll("response: #respHeaders, request: #reqHeaders - #reqBody")
    def "when translating responses with uncommon headers"(){

        given: "Origin service returns body of type " + respHeaders
        def xmlResp = { request -> return new Response(200, "OK", respHeaders, respBody) }


        when: "User sends requests through repose"
        def resp = deproxy.makeRequest(url:(String) reposeEndpoint, method:method, headers:reqHeaders, requestBody:reqBody, defaultHandler:xmlResp)

        then: "Response body should contain"
        !resp.receivedResponse.body.contains(notContains)
        !resp.handlings[0].request.body.contains(notContains)

        and: "Response code should be"
        resp.receivedResponse.code.equalsIgnoreCase("200")

        where:
        respHeaders                                  | reqHeaders                                    | notContains    | respBody     | reqBody       | method
        weirdContentTypeAtomXML                      | acceptXML                                     | "rootPassword" | respXMLBody  | ""            | "GET"
        weirdContentTypeAtomXML + weirdAcceptAtomXML | acceptXML                                     | "rootPassword" | respXMLBody  | ""            | "GET"
        weirdContentTypeAtomXML + weirdAcceptAtomXML | acceptXML + contentTypeAtomXML                | "rootPassword" | respXMLBody  | respXMLBody   | "POST"
        weirdContentTypeAtomXML + weirdAcceptAtomXML | weirdAcceptAtomXML + weirdContentTypeAtomXML  | "rootPassword" | respXMLBody  | respXMLBody   | "GET"
        weirdContentTypeXML                          | acceptXML                                     | "rootPassword" | respXMLBody  | ""            | "GET"
        weirdContentTypeXML + weirdAcceptXML         | acceptXML                                     | "rootPassword" | respXMLBody  | ""            | "GET"
        weirdContentTypeXML + weirdAcceptXML         | acceptXML + contentTypeXML                    | "rootPassword" | respXMLBody  | respXMLBody   | "POST"
        weirdContentTypeXML + weirdAcceptXML         | weirdAcceptXML + weirdContentTypeXML          | "rootPassword" | respXMLBody  | respXMLBody   | "GET"
    }
}
