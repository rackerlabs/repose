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
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Filters
import spock.lang.Unroll

/**
 * Created by jennyvo on 5/31/16.
 *  Versioning for identity when config versioning with json-format="IDENTITY"
 */
@Category(Filters)
class VersioningForIdentityTest extends ReposeValveTest {
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
        repose.configurationProvider.applyConfigs("features/filters/versioning/identity", params)
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

    def "verify the response JSON is formatted for Identity when configured as such for a versions request"() {
        when: "User sends requests through repose"
        def mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: acceptJSON)

        then: "Response body should contain the expected JSON format"
        mc.receivedResponse.code == "200"

        def json = new JsonSlurper().parseText(mc.receivedResponse.body as String)
        json.versions.values
        json.versions.values.size == 5
        json.versions.values[0].id
        json.versions.values[0].id.contains("/v")
        json.versions.values[0].links
        json.versions.values[0].links.href
        json.versions.values[0].links.rel
        json.versions.values[0]."media-types"
        json.versions.values[0]."media-types".base
        json.versions.values[0]."media-types".base[0].contains("application/")
        json.versions.values[0]."media-types".type
        json.versions.values[0]."media-types".type[0].contains("application/")
        json.versions.values[0].status
        json.versions.values.find { it.id == '/v1' }.status == "deprecated"
        json.versions.values.find { it.id == '/v2' }.status == "stable"
        json.versions.values.find { it.id == '/v3' }.status == "deprecated"
        json.versions.values.find { it.id == '/v4' }.status == "alpha"
        json.versions.values.find { it.id == '/v5' }.status == "beta"
    }

    def "verify the response JSON is formatted for Identity when configured as such for a single version request"() {
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
        json.version.status == "deprecated"
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
}
