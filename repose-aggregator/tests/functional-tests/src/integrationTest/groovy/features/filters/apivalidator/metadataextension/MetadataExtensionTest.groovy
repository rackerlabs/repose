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
package features.filters.apivalidator.metadataextension

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.XmlParsing
import spock.lang.Unroll

@Category(XmlParsing)
class MetadataExtensionTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/metadataextension", params)

        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    // Standard/Custom GET's are allowed(200) regardless of roles.
    @Unroll("Method: GET, Path: #path, Roles: #headers, and respcode: 200")
    def "Test positive metadata extension access with GET"() {
        given:
        MessageChain mc

        when:
        mc = deproxy.makeRequest(url: reposeEndpoint + "/" + path, headers: headers)

        then:
        mc.getReceivedResponse().getCode().equals("200")

        where:
        [path, headers] << [["standard/metadata/foo",
                             "standard/metadata/billing:foo",
                             "standard/metadata/service:foo",
                             "custom/metadata/foo",
                             "custom/metadata/customer:role:foo",
                             "custom/metadata/service:foo",
                             "custom/metadata/%7C%7C%7C:foo"],
                            [["x-roles": "repose_test, admin"],
                             ["x-roles": "repose_test, billing:role"],
                             ["x-roles": "repose_test, service:role"],
                             ["x-roles": "repose_test, superAdmin"],
                             ["x-roles": "repose_test, customer:role"],
                             ["x-roles": "repose_test, another_role"],
                             ["x-roles": "repose_test, ???"]]].combinations()
    }

    // Standard/Custom POST's are not allowed(405) regardless of roles.
    @Unroll("Method: POST, Path: #path, Roles: #headers, and respcode: 405")
    def "Test negative metadata extension access with POST"() {
        given:
        MessageChain mc

        when:
        mc = deproxy.makeRequest(method: "POST", url: reposeEndpoint + "/" + path, headers: headers)

        then:
        mc.getReceivedResponse().getCode().equals("405")

        where:
        [path, headers] << [["standard/metadata/foo",
                             "standard/metadata/billing:foo",
                             "standard/metadata/service:foo",
                             "custom/metadata/foo",
                             "custom/metadata/customer:role:foo",
                             "custom/metadata/service:foo",
                             "custom/metadata/%7C%7C%7C:foo"],
                            [["x-roles": "repose_test, admin"],
                             ["x-roles": "repose_test, billing:role"],
                             ["x-roles": "repose_test, service:role"],
                             ["x-roles": "repose_test, superAdmin"],
                             ["x-roles": "repose_test, customer:role"],
                             ["x-roles": "repose_test, another_role"],
                             ["x-roles": "repose_test, ???"]]].combinations()

    }

    // Standard PUT's and DELETE's are allowed or forbidden(403) based on roles.
    // IF the list of roles contains the target
    // OR the list of roles contains admin,
    // THEN access should be allowed(200);
    // ELSE access should be forbidden(403).
    //
    // AND
    //
    // Custom PUT's and DELETE's are allowed or forbidden(403) based on roles.
    // IF the list of roles contains the target
    // OR the list of roles contains admin OR superAdmin,
    // - OR -
    // IF the target is the overloaded target
    // AND the list of roles contains the overloaded role,
    // - OR -
    // IF the target is the URL Encoded regex target
    // AND the list of roles contains the question marks role,
    // THEN access should be allowed(200);
    // ELSE access should be forbidden(403).
    @Unroll("Method: #method, Path: #path, Roles: #headers, and respcode: #responseCode")
    def "Test positive metadata extension access with #method"() {
        given:
        MessageChain mc
        def requestBody = """<?xml version='1.0' encoding='UTF-8'?>
        |<metadata xmlns="http://docs.rackspace.com/metadata/api">
        |  <meta key="name">test_server</meta>
        |  <meta key="type">test</meta>
        |</metadata>""".stripMargin()

        when:
        headers.put("content-type", "application/xml")
        mc = deproxy.makeRequest(method: method, url: reposeEndpoint + "/" + path, headers: headers, requestBody: requestBody)

        then:
        mc.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | path                                | headers                                   | responseCode
        "PUT"    | "standard/metadata/foo"             | ["x-roles": "repose_test, admin"]         | "200"
        "PUT"    | "standard/metadata/billing:foo"     | ["x-roles": "repose_test, admin"]         | "200"
        "PUT"    | "standard/metadata/service:foo"     | ["x-roles": "repose_test, admin"]         | "200"
        "PUT"    | "custom/metadata/foo"               | ["x-roles": "repose_test, admin"]         | "200"
        "PUT"    | "custom/metadata/customer:role:foo" | ["x-roles": "repose_test, admin"]         | "200"
        "PUT"    | "custom/metadata/service:foo"       | ["x-roles": "repose_test, admin"]         | "200"
        "PUT"    | "custom/metadata/%7C%7C%7C:foo"     | ["x-roles": "repose_test, admin"]         | "200"

        "PUT"    | "standard/metadata/foo"             | ["x-roles": "repose_test, billing:role"]  | "403"
        "PUT"    | "standard/metadata/billing:foo"     | ["x-roles": "repose_test, billing:role"]  | "200"
        "PUT"    | "standard/metadata/service:foo"     | ["x-roles": "repose_test, billing:role"]  | "403"
        "PUT"    | "custom/metadata/foo"               | ["x-roles": "repose_test, billing:role"]  | "403"
        "PUT"    | "custom/metadata/customer:role:foo" | ["x-roles": "repose_test, billing:role"]  | "403"
        "PUT"    | "custom/metadata/service:foo"       | ["x-roles": "repose_test, billing:role"]  | "403"
        "PUT"    | "custom/metadata/%7C%7C%7C:foo"     | ["x-roles": "repose_test, billing:role"]  | "403"

        "PUT"    | "standard/metadata/foo"             | ["x-roles": "repose_test, service:role"]  | "403"
        "PUT"    | "standard/metadata/billing:foo"     | ["x-roles": "repose_test, service:role"]  | "403"
        "PUT"    | "standard/metadata/service:foo"     | ["x-roles": "repose_test, service:role"]  | "200"
        "PUT"    | "custom/metadata/foo"               | ["x-roles": "repose_test, service:role"]  | "403"
        "PUT"    | "custom/metadata/customer:role:foo" | ["x-roles": "repose_test, service:role"]  | "403"
        "PUT"    | "custom/metadata/service:foo"       | ["x-roles": "repose_test, service:role"]  | "200"
        "PUT"    | "custom/metadata/%7C%7C%7C:foo"     | ["x-roles": "repose_test, service:role"]  | "403"

        "PUT"    | "standard/metadata/foo"             | ["x-roles": "repose_test, superAdmin"]    | "403"
        "PUT"    | "standard/metadata/billing:foo"     | ["x-roles": "repose_test, superAdmin"]    | "403"
        "PUT"    | "standard/metadata/service:foo"     | ["x-roles": "repose_test, superAdmin"]    | "403"
        "PUT"    | "custom/metadata/foo"               | ["x-roles": "repose_test, superAdmin"]    | "200"
        "PUT"    | "custom/metadata/customer:role:foo" | ["x-roles": "repose_test, superAdmin"]    | "200"
        "PUT"    | "custom/metadata/service:foo"       | ["x-roles": "repose_test, superAdmin"]    | "200"
        "PUT"    | "custom/metadata/%7C%7C%7C:foo"     | ["x-roles": "repose_test, superAdmin"]    | "200"

        "PUT"    | "standard/metadata/foo"             | ["x-roles": "repose_test, customer:role"] | "403"
        "PUT"    | "standard/metadata/billing:foo"     | ["x-roles": "repose_test, customer:role"] | "403"
        "PUT"    | "standard/metadata/service:foo"     | ["x-roles": "repose_test, customer:role"] | "403"
        "PUT"    | "custom/metadata/foo"               | ["x-roles": "repose_test, customer:role"] | "403"
        "PUT"    | "custom/metadata/customer:role:foo" | ["x-roles": "repose_test, customer:role"] | "200"
        "PUT"    | "custom/metadata/service:foo"       | ["x-roles": "repose_test, customer:role"] | "403"
        "PUT"    | "custom/metadata/%7C%7C%7C:foo"     | ["x-roles": "repose_test, customer:role"] | "403"

        "PUT"    | "standard/metadata/foo"             | ["x-roles": "repose_test, another_role"]  | "403"
        "PUT"    | "standard/metadata/billing:foo"     | ["x-roles": "repose_test, another_role"]  | "403"
        "PUT"    | "standard/metadata/service:foo"     | ["x-roles": "repose_test, another_role"]  | "403"
        "PUT"    | "custom/metadata/foo"               | ["x-roles": "repose_test, another_role"]  | "403"
        "PUT"    | "custom/metadata/customer:role:foo" | ["x-roles": "repose_test, another_role"]  | "403"
        "PUT"    | "custom/metadata/service:foo"       | ["x-roles": "repose_test, another_role"]  | "200"
        "PUT"    | "custom/metadata/%7C%7C%7C:foo"     | ["x-roles": "repose_test, another_role"]  | "403"

        "PUT"    | "standard/metadata/foo"             | ["x-roles": "repose_test, ???"]           | "403"
        "PUT"    | "standard/metadata/billing:foo"     | ["x-roles": "repose_test, ???"]           | "403"
        "PUT"    | "standard/metadata/service:foo"     | ["x-roles": "repose_test, ???"]           | "403"
        "PUT"    | "custom/metadata/foo"               | ["x-roles": "repose_test, ???"]           | "403"
        "PUT"    | "custom/metadata/customer:role:foo" | ["x-roles": "repose_test, ???"]           | "403"
        "PUT"    | "custom/metadata/service:foo"       | ["x-roles": "repose_test, ???"]           | "403"
        "PUT"    | "custom/metadata/%7C%7C%7C:foo"     | ["x-roles": "repose_test, ???"]           | "200"

        "DELETE" | "standard/metadata/foo"             | ["x-roles": "repose_test, admin"]         | "200"
        "DELETE" | "standard/metadata/billing:foo"     | ["x-roles": "repose_test, admin"]         | "200"
        "DELETE" | "standard/metadata/service:foo"     | ["x-roles": "repose_test, admin"]         | "200"
        "DELETE" | "custom/metadata/foo"               | ["x-roles": "repose_test, admin"]         | "200"
        "DELETE" | "custom/metadata/customer:role:foo" | ["x-roles": "repose_test, admin"]         | "200"
        "DELETE" | "custom/metadata/service:foo"       | ["x-roles": "repose_test, admin"]         | "200"
        "DELETE" | "custom/metadata/%7C%7C%7C:foo"     | ["x-roles": "repose_test, admin"]         | "200"

        "DELETE" | "standard/metadata/foo"             | ["x-roles": "repose_test, billing:role"]  | "403"
        "DELETE" | "standard/metadata/billing:foo"     | ["x-roles": "repose_test, billing:role"]  | "200"
        "DELETE" | "standard/metadata/service:foo"     | ["x-roles": "repose_test, billing:role"]  | "403"
        "DELETE" | "custom/metadata/foo"               | ["x-roles": "repose_test, billing:role"]  | "403"
        "DELETE" | "custom/metadata/customer:role:foo" | ["x-roles": "repose_test, billing:role"]  | "403"
        "DELETE" | "custom/metadata/service:foo"       | ["x-roles": "repose_test, billing:role"]  | "403"
        "DELETE" | "custom/metadata/%7C%7C%7C:foo"     | ["x-roles": "repose_test, billing:role"]  | "403"

        "DELETE" | "standard/metadata/foo"             | ["x-roles": "repose_test, service:role"]  | "403"
        "DELETE" | "standard/metadata/billing:foo"     | ["x-roles": "repose_test, service:role"]  | "403"
        "DELETE" | "standard/metadata/service:foo"     | ["x-roles": "repose_test, service:role"]  | "200"
        "DELETE" | "custom/metadata/foo"               | ["x-roles": "repose_test, service:role"]  | "403"
        "DELETE" | "custom/metadata/customer:role:foo" | ["x-roles": "repose_test, service:role"]  | "403"
        "DELETE" | "custom/metadata/service:foo"       | ["x-roles": "repose_test, service:role"]  | "200"
        "DELETE" | "custom/metadata/%7C%7C%7C:foo"     | ["x-roles": "repose_test, service:role"]  | "403"

        "DELETE" | "standard/metadata/foo"             | ["x-roles": "repose_test, superAdmin"]    | "403"
        "DELETE" | "standard/metadata/billing:foo"     | ["x-roles": "repose_test, superAdmin"]    | "403"
        "DELETE" | "standard/metadata/service:foo"     | ["x-roles": "repose_test, superAdmin"]    | "403"
        "DELETE" | "custom/metadata/foo"               | ["x-roles": "repose_test, superAdmin"]    | "200"
        "DELETE" | "custom/metadata/customer:role:foo" | ["x-roles": "repose_test, superAdmin"]    | "200"
        "DELETE" | "custom/metadata/service:foo"       | ["x-roles": "repose_test, superAdmin"]    | "200"
        "DELETE" | "custom/metadata/%7C%7C%7C:foo"     | ["x-roles": "repose_test, superAdmin"]    | "200"

        "DELETE" | "standard/metadata/foo"             | ["x-roles": "repose_test, customer:role"] | "403"
        "DELETE" | "standard/metadata/billing:foo"     | ["x-roles": "repose_test, customer:role"] | "403"
        "DELETE" | "standard/metadata/service:foo"     | ["x-roles": "repose_test, customer:role"] | "403"
        "DELETE" | "custom/metadata/foo"               | ["x-roles": "repose_test, customer:role"] | "403"
        "DELETE" | "custom/metadata/customer:role:foo" | ["x-roles": "repose_test, customer:role"] | "200"
        "DELETE" | "custom/metadata/service:foo"       | ["x-roles": "repose_test, customer:role"] | "403"
        "DELETE" | "custom/metadata/%7C%7C%7C:foo"     | ["x-roles": "repose_test, customer:role"] | "403"

        "DELETE" | "standard/metadata/foo"             | ["x-roles": "repose_test, another_role"]  | "403"
        "DELETE" | "standard/metadata/billing:foo"     | ["x-roles": "repose_test, another_role"]  | "403"
        "DELETE" | "standard/metadata/service:foo"     | ["x-roles": "repose_test, another_role"]  | "403"
        "DELETE" | "custom/metadata/foo"               | ["x-roles": "repose_test, another_role"]  | "403"
        "DELETE" | "custom/metadata/customer:role:foo" | ["x-roles": "repose_test, another_role"]  | "403"
        "DELETE" | "custom/metadata/service:foo"       | ["x-roles": "repose_test, another_role"]  | "200"
        "DELETE" | "custom/metadata/%7C%7C%7C:foo"     | ["x-roles": "repose_test, another_role"]  | "403"

        "DELETE" | "standard/metadata/foo"             | ["x-roles": "repose_test, ???"]           | "403"
        "DELETE" | "standard/metadata/billing:foo"     | ["x-roles": "repose_test, ???"]           | "403"
        "DELETE" | "standard/metadata/service:foo"     | ["x-roles": "repose_test, ???"]           | "403"
        "DELETE" | "custom/metadata/foo"               | ["x-roles": "repose_test, ???"]           | "403"
        "DELETE" | "custom/metadata/customer:role:foo" | ["x-roles": "repose_test, ???"]           | "403"
        "DELETE" | "custom/metadata/service:foo"       | ["x-roles": "repose_test, ???"]           | "403"
        "DELETE" | "custom/metadata/%7C%7C%7C:foo"     | ["x-roles": "repose_test, ???"]           | "200"
    }
}
