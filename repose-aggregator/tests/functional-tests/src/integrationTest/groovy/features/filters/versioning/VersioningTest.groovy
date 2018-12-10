/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package features.filters.versioning

import groovy.json.JsonSlurper
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * User: dimi5963
 * Date: 9/11/13
 * Time: 5:03 PM
 */
class VersioningTest extends ReposeValveTest {
    def static Map acceptXML = [accept: "application/xml"]
    def static Map acceptJSON = [accept: "application/json"]
    def static Map acceptV1XML = [accept: 'application/v1+xml']
    def static Map acceptV2XML = [accept: 'application/v2+xml']
    def static Map acceptV1JSON = [accept: 'application/v1+json']
    def static Map acceptV2JSON = [accept: 'application/v2+json']
    def static Map acceptHtml = [accept: 'text/html']
    def static Map acceptXHtml = [accept: 'application/xhtml+xml']
    def static Map acceptXMLWQ = [accept: 'application/xml;q=0.9,*/*;q=0.8']
    def static Map acceptV2VendorJSON = [accept: 'application/vnd.vendor.service-v2+json']
    def static Map acceptV2VendorJSON2 = [accept: 'application/vnd.vendor.service+json; version=2']
    def static Map acceptV2VendorXML = [accept: 'application/vnd.vendor.service+xml; version=2']
    def static Map acceptV2VendorXML2 = [accept: 'application/vnd.vendor.service-v2+xml']
    def static Map acceptV1VendorJSON = [accept: 'application/vnd.vendor.service-v1+json']
    def static Map acceptV1VendorJSON2 = [accept: 'application/vnd.vendor.service+json; version=1']
    def static Map acceptV1VendorXML = [accept: 'application/vnd.vendor.service+xml; version=1']
    def static Map acceptV1VendorXML2 = [accept: 'application/vnd.vendor.service-v1+xml']

    def static Map contentXML = ["content-type": "application/xml"]
    def static Map contentJSON = ["content-type": "application/json"]

    //Start repose once for this particular translation test
    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        deproxy.addEndpoint(properties.targetPort2)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/versioning", params)
        repose.start()
    }

    @Unroll
    def "when retrieving all versions: #reqHeaders"() {
        when: "User sends requests through repose"
        def mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: reqHeaders)

        then: "Response body should contain"
        mc.receivedResponse.code == "200"

        for (String st : shouldContain) {
            mc.receivedResponse.body.contains(st)
        }

        where:
        reqHeaders | shouldContain
        acceptXML  | ['id="/v1"', 'id="/v2"']
        acceptJSON | ['"id" : "/v1"', '"id" : "/v2"']
    }

    // http://developer.openstack.org/api-ref-compute-v2.1.html#listVersionsv2.1
    def "verify the response JSON is formatted for Compute when the format is not configured for a versions request"() {
        when: "User sends requests through repose"
        def mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: acceptJSON)

        then: "Response body should contain the expected JSON format"
        mc.receivedResponse.code == "200"

        def json = new JsonSlurper().parseText(mc.receivedResponse.body as String)
        json.versions
        json.versions.size == 5
        json.versions[0].id
        json.versions[0].id.contains("/v")
        json.versions[0].links
        json.versions[0].links.href
        json.versions[0].links.rel
        json.versions[0]."media-types"
        json.versions[0]."media-types".base
        json.versions[0]."media-types".base[0].contains("application/")
        json.versions[0]."media-types".type
        json.versions[0]."media-types".type[0].contains("application/")
        json.versions[0].status
        (json.versions[0].status as String).toUpperCase() == json.versions[0].status as String
        json.versions.find { it.id == '/v1' }.status == "DEPRECATED"
        json.versions.find { it.id == '/v2' }.status == "CURRENT"
        json.versions.find { it.id == '/v3' }.status == "DEPRECATED"
        json.versions.find { it.id == '/v4' }.status == "ALPHA"
        json.versions.find { it.id == '/v5' }.status == "BETA"
    }

    @Unroll
    def "when retrieving version details: #reqHeaders - #requestUri"() {
        when: "User sends requests through repose"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUri, method: 'GET', headers: reqHeaders)

        then: "Response body should contain"
        mc.receivedResponse.code == respCode

        for (String st : shouldContain) {
            mc.receivedResponse.body.contains(st)
        }

        for (String st : shouldNotContain) {
            !mc.receivedResponse.body.contains(st)
        }

        where:
        reqHeaders   | respCode | shouldContain                    | shouldNotContain | requestUri
        acceptJSON   | '200'    | ['"id" : "/v1"']                 | ['"id" : "/v2"'] | "/v1"
        acceptJSON   | '200'    | ['"id" : "/v2"']                 | ['"id" : "/v1"'] | "/v2"
        acceptV1JSON | '200'    | ['"id" : "/v1"']                 | ['"id" : "/v2"'] | "/v1"
        acceptV2JSON | '200'    | ['"id" : "/v2"']                 | ['"id" : "/v1"'] | "/v2"
        acceptJSON   | '300'    | ['"id" : "/v2"', '"id" : "/v1"'] | []               | "/wrong"
        acceptJSON   | '300'    | ['"id" : "/v2"', '"id" : "/v1"'] | []               | "/0/usertest1/ss"
        acceptXML    | '200'    | ['id="/v1"']                     | ['id="/v2"']     | "/v1"
        acceptXML    | '200'    | ['id="/v2"']                     | ['id="/v1"']     | "/v2"
        acceptV1XML  | '200'    | ['id="/v1"']                     | ['id="/v1"']     | "/v1"
        acceptV2XML  | '200'    | ['id="/v2"']                     | ['id="/v2"']     | "/v2"
        acceptXML    | '300'    | ['id="/v2"', 'id="/v1"']         | []               | "/wrong"
        acceptXML    | '300'    | ['id="/v2"', 'id="/v1"']         | []               | "/v1xxx/usertest1/ss"
        acceptXML    | '300'    | ['id="/v2"', 'id="/v1"']         | []               | "/0/usertest1/ss"
        acceptJSON   | '300'    | ['id="/v2"', 'id="/v1"']         | []               | "/v1xxx/usertest1/ss"
    }

    // http://developer.openstack.org/api-ref-compute-v2.1.html#showVersionsv2.1
    def "verify the response JSON is formatted for Compute when the format is not configured for a single version request"() {
        when: "User sends requests through repose"
        def mc = deproxy.makeRequest(url: reposeEndpoint + "/v1", method: 'GET', headers: acceptJSON)

        then: "Response body should contain the expected JSON format"
        mc.receivedResponse.code == "200"

        def json = new JsonSlurper().parseText(mc.receivedResponse.body as String)
        json.version
        json.version.id == "/v1"
        json.version."media-types"
        json.version."media-types".base
        json.version."media-types".base[0].contains("application/")
        json.version."media-types".type
        json.version."media-types".type[0].contains("application/")
        json.version."media-types".find { it.type == "application/v1+xml" }.base == "application/xml"
        json.version."media-types".find { it.type == "application/v1+json" }.base == "application/json"
        json.version.status == "DEPRECATED"
    }

    @Unroll
    def "when retrieving version details with variant uri: #reqHeaders - #requestUri"() {
        when: "User sends requests through repose"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUri, method: 'GET', headers: reqHeaders)

        then: "Response body should contain"
        mc.receivedResponse.code == "200"

        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("host") == host

        where:
        reqHeaders          | requestUri         | host
        acceptV2VendorJSON  | "/usertest1/ss"    | "localhost:" + properties.targetPort2
        acceptV2VendorXML   | "/usertest1/ss"    | "localhost:" + properties.targetPort2
        acceptV2VendorJSON2 | "/usertest1/ss"    | "localhost:" + properties.targetPort2
        acceptV2VendorXML2  | "/usertest1/ss"    | "localhost:" + properties.targetPort2
        acceptHtml          | "/v2/usertest1/ss" | "localhost:" + properties.targetPort2
        acceptXHtml         | "/v2/usertest1/ss" | "localhost:" + properties.targetPort2
        acceptXMLWQ         | "/v2/usertest1/ss" | "localhost:" + properties.targetPort2
        acceptV1VendorJSON  | "/usertest1/ss"    | "localhost:" + properties.targetPort
        acceptV1VendorXML   | "/usertest1/ss"    | "localhost:" + properties.targetPort
        acceptV1VendorJSON2 | "/usertest1/ss"    | "localhost:" + properties.targetPort
        acceptV1VendorXML2  | "/usertest1/ss"    | "localhost:" + properties.targetPort
        acceptXML           | "/v1/usertest1/ss" | "localhost:" + properties.targetPort
        acceptJSON          | "/v1/usertest1/ss" | "localhost:" + properties.targetPort
    }

    def "Should split request headers according to rfc by default"() {
        given:
        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders = [
                "user-agent": userAgentValue,
                "x-pp-user" : "usertest1, usertest2, usertest3",
                accept      : "application/xml;q=1 , application/json;q=0.5"]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v2/test", method: 'GET', headers: reqHeaders)

        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.getHeaders().findAll("user-agent").size() == 1
        mc.handlings[0].request.headers['user-agent'] == userAgentValue
        mc.handlings[0].request.getHeaders().findAll("x-pp-user").size() == 3
        mc.handlings[0].request.getHeaders().findAll("accept").size() == 2
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = [
                location: "http://somehost.com/blah?a=b,c,d",
                via     : "application/xml;q=0.3, application/json;q=1"]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v2/test", method: 'GET', defaultHandler: {
            new Response(201, "Created", respHeaders, "")
        })

        then:
        mc.receivedResponse.code == "201"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers['location'] == "$reposeEndpoint/v2/blah?a=b,c,d"
        mc.receivedResponse.headers.findAll("via").size() == 1
    }

    @Unroll
    def "Requests - headers: #headerName with \"#headerValue\" keep its case in requests"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0',
                (headerName)    : headerValue]

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v2/test", headers: headers)

        then: "the request should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains(headerName)
        mc.handlings[0].request.headers.getFirstValue(headerName) == headerValue

        where:
        headerName         | headerValue
        "Accept"           | "text/plain"
        "ACCEPT"           | "text/PLAIN"
        "accept"           | "TEXT/plain;q=0.2"
        "aCCept"           | "text/plain"
        "CONTENT-Encoding" | "identity"
        "Content-ENCODING" | "identity"
    }

    @Unroll
    def "Responses - headers: #headerName with \"#headerValue\" keep its case in responses"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0',
                (headerName)    : headerValue
        ]

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v2/test", defaultHandler: {
            new Response(200, null, headers)
        })

        then: "the response should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.contains(headerName)
        mc.receivedResponse.headers.getFirstValue(headerName) == headerValue

        where:
        headerName     | headerValue
        "x-auth-token" | "123445"
        "X-AUTH-TOKEN" | "239853"
        "x-AUTH-token" | "slDSFslk&D"
        "x-auth-TOKEN" | "sl4hsdlg"
        "CONTENT-Type" | "application/json"
        "Content-TYPE" | "application/json"
    }
}
