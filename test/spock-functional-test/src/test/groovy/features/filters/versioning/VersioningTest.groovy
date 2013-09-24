package features.filters.versioning

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import spock.lang.Unroll

/**
 * User: dimi5963
 * Date: 9/11/13
 * Time: 5:03 PM
 */
class VersioningTest extends ReposeValveTest {
    def static Map acceptXML = ["accept": "application/xml"]
    def static Map acceptJSON = ["accept": "application/json"]
    def static Map acceptV1XML = ['accept': 'application/v1+xml']
    def static Map acceptV2XML = ['accept': 'application/v2+xml']
    def static Map acceptV1JSON = ['accept': 'application/v1+json']
    def static Map acceptV2JSON = ['accept': 'application/v2+json']
    def static Map acceptHtml = ['accept': 'text/html']
    def static Map acceptXHtml = ['accept': 'application/xhtml+xml']
    def static Map acceptXMLWQ = ['accept': 'application/xml;q=0.9,*/*;q=0.8']
    def static Map acceptV2VendorJSON = ['accept': 'application/vnd.vendor.service-v2+json']
    def static Map acceptV2VendorXML = ['accept': 'application/vnd.vendor.service+xml; version=2']
    def static Map acceptV1VendorJSON = ['accept': 'application/vnd.vendor.service-v1+json']
    def static Map acceptV1VendorXML = ['accept': 'application/vnd.vendor.service+xml; version=1']

    def static Map contentXML = ["content-type": "application/xml"]
    def static Map contentJSON = ["content-type": "application/json"]


    //Start repose once for this particular translation test
    def setupSpec() {

        repose.applyConfigs(
                "features/filters/versioning"
        )
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
        deproxy.addEndpoint(properties.getProperty("target.port2").toInteger())
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    @Unroll("request: #reqHeaders")
    def "when retrieving all versions"() {
        when: "User sends requests through repose"
        def mc = deproxy.makeRequest((String) reposeEndpoint, 'GET', reqHeaders)

        then: "Response body should contain"
        mc.receivedResponse.code == "200"

        for (String st : shouldContain) {
            mc.receivedResponse.body.contains(st)
        }

        where:
        reqHeaders | shouldContain
        acceptXML  | ['id="/v1"','id="/v2"']
        acceptJSON | ['"id" : "/v1"','"id" : "/v2"']


    }

    @Unroll("request: #reqHeaders - #requestUri")
    def "when retrieving version details"() {
        when: "User sends requests through repose"
        def mc = deproxy.makeRequest((String) reposeEndpoint + requestUri, 'GET', reqHeaders)

        then: "Response body should contain"
        mc.receivedResponse.code == respCode

        for (String st : shouldContain) {
            mc.receivedResponse.body.contains(st)
        }

        for (String st : shouldNotContain) {
            !mc.receivedResponse.body.contains(st)
        }

        where:
        reqHeaders            | respCode | shouldContain                   | shouldNotContain | requestUri
        acceptJSON            | '200'    | ['"id" : "/v1"']                | ['"id" : "/v2"'] | "/v1"
        acceptJSON            | '200'    | ['"id" : "/v2"']                | ['"id" : "/v1"'] | "/v2"
        acceptV1JSON          | '200'    | ['"id" : "/v1"']                | ['"id" : "/v2"'] | "/v1"
        acceptV2JSON          | '200'    | ['"id" : "/v2"']                | ['"id" : "/v1"'] | "/v2"
        acceptJSON            | '300'    | ['"id" : "/v2"','"id" : "/v1"'] | []               | "/wrong"
        acceptJSON            | '300'    | ['"id" : "/v2"','"id" : "/v1"'] | []               | "/0/usertest1/ss"
        acceptXML             | '200'    | ['id="/v1"']                    | ['id="/v2"']     | "/v1"
        acceptXML             | '200'    | ['id="/v2"']                    | ['id="/v1"']     | "/v2"
        acceptV1XML           | '200'    | ['id="/v1"']                    | ['id="/v1"']     | "/v1"
        acceptV2XML           | '200'    | ['id="/v2"']                    | ['id="/v2"']     | "/v2"
        acceptXML             | '300'    | ['id="/v2"','id="/v1"']         | []               | "/wrong"
        acceptXML             | '300'    | ['id="/v2"','id="/v1"']         | []               | "/v1xxx/usertest1/ss"
        acceptXML             | '300'    | ['id="/v2"','id="/v1"']         | []               | "/0/usertest1/ss"
        acceptJSON            | '300'    | ['id="/v2"','id="/v1"']         | []               | "/v1xxx/usertest1/ss"
    }

    @Unroll("request: #reqHeaders - #requestUri")
    def "when retrieving version details with variant uri"() {
        when: "User sends requests through repose"
        def mc = deproxy.makeRequest((String) reposeEndpoint + requestUri, 'GET', reqHeaders)

        then: "Response body should contain"
        mc.receivedResponse.code == "200"

        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("host") == host

        where:
        reqHeaders            | requestUri         | host
        acceptV2VendorJSON    | "/usertest1/ss"    | "localhost:" + properties.getProperty("target.port2").toInteger()
        acceptV2VendorXML     | "/usertest1/ss"    | "localhost:" + properties.getProperty("target.port2").toInteger()
        acceptHtml            | "/v2/usertest1/ss" | "localhost:" + properties.getProperty("target.port2").toInteger()
        acceptXHtml           | "/v2/usertest1/ss" | "localhost:" + properties.getProperty("target.port2").toInteger()
        acceptXMLWQ           | "/v2/usertest1/ss" | "localhost:" + properties.getProperty("target.port2").toInteger()
        acceptV1VendorJSON    | "/usertest1/ss"    | "localhost:" + properties.getProperty("target.port").toInteger()
        acceptV1VendorXML     | "/usertest1/ss"    | "localhost:" + properties.getProperty("target.port").toInteger()
        acceptXML             | "/v1/usertest1/ss" | "localhost:" + properties.getProperty("target.port").toInteger()
        acceptJSON            | "/v1/usertest1/ss" | "localhost:" + properties.getProperty("target.port").toInteger()


    }
}
