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
package features.filters.attributemappingvalidation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

import static javax.ws.rs.core.MediaType.TEXT_PLAIN

/**
 * Created by adrian on 5/10/17.
 */
class AttributeMappingPolicyValidationFilterTest extends ReposeValveTest {

    final static String TEXT_YAML = "text/yaml"

    static ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory())

    def setupSpec() {
        reposeLogSearch.cleanLog()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/attributemappingvalidation", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll
    def "#method should hit origin service #times times"() {
        given:
        Map headers = body ? ["content-type": TEXT_PLAIN] : [:]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: method, headers: headers, requestBody: body)

        then:
        mc.handlings.size() == times
        if (method != "PUT") {
            assert mc.handlings[0].request.body == body
        }

        where:
        method   | body     | times
        "GET"    | ""       | 1
        "POST"   | "banana" | 1
        "DELETE" | ""       | 1
        "PUT"    | "banana" | 0
    }

    def "should validate correct JSON"() {
        given:
        String body =
            """
            |---
            |mapping:
            |  rules:
            |  - local:
            |      user:
            |        domain: "{D}"
            |        name: "{D}"
            |        email: "{D}"
            |        roles: "{D}"
            |        expire: "{D}"
            |  version: RAX-1
            """.stripMargin()

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "PUT", headers: ["content-type": TEXT_YAML], requestBody: body)

        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        yamlMapper.readTree(body) == yamlMapper.readTree(mc.handlings[0].request.body as String)
    }

    def "should not remove the name attribute from a remote in a YAML policy"() {
        given:
        String body =
            """
            |---
            |mapping:
            |  rules:
            |  - local:
            |      user:
            |        name: "{D}"
            |    remote:
            |    - multiValue: false
            |      name: Username
            |      regex: false
            |  version: RAX-1
            """.stripMargin()

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "PUT", headers: ["content-type": TEXT_YAML], requestBody: body)

        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        yamlMapper.readTree(body) == yamlMapper.readTree(mc.handlings[0].request.body as String)
    }

    def "should fail to validate bad YAML"() {
        given:
        String body =
            """
            |---
            |mapping:
            |  rules:
            |    local:
            |      user:
            |        domain: "{D}"
            |        name: "{D}"
            |        email: "{D}"
            |        roles: "{D}"
            |        expire: "{D}"
            |  version: RAX-1
            """.stripMargin()

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "PUT", headers: ["content-type": TEXT_YAML], requestBody: body)

        then:
        mc.receivedResponse.code == "400"
        mc.handlings.size() == 0
    }
}
