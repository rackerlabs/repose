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

import groovy.json.JsonSlurper
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.yaml.snakeyaml.Yaml
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.*
import static javax.ws.rs.core.MediaType.*

/**
 * Created by adrian on 5/10/17.
 */
class AttributeMappingPolicyValidationFilterTest extends ReposeValveTest {

    final static String TEXT_YAML = "text/yaml"

    static XmlSlurper xmlSlurper = new XmlSlurper()
    static JsonSlurper jsonSlurper = new JsonSlurper()
    static Yaml yaml = new Yaml()

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

    @Unroll
    def "#contentType should be rejected with a 415"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "PUT", headers: ["content-type": contentType], requestBody: "Lorem ipsum")

        then:
        mc.receivedResponse.code.toInteger() == SC_UNSUPPORTED_MEDIA_TYPE

        where:
        contentType << [TEXT_PLAIN]
    }

    def "should validate correct XML"() {
        given:
        String body =
            """<?xml version="1.0" encoding="UTF-8"?>
            |<mapping xmlns="http://docs.rackspace.com/identity/api/ext/MappingRules"
            |         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            |         xmlns:xs="http://www.w3.org/2001/XMLSchema"
            |         xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
            |         version="RAX-1">
            |   <rules>
            |      <rule>
            |        <local>
            |            <user>
            |               <name value="{D}"/>
            |               <email value="{D}"/>
            |               <expire value="{D}"/>
            |               <domain value="{D}"/>
            |               <roles value="{D}"/>
            |            </user>
            |         </local>
            |      </rule>
            |   </rules>
            |</mapping>
            |""".stripMargin()

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "PUT", headers: ["content-type": APPLICATION_XML], requestBody: body)

        then:
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings.size() == 1
        xmlSlurper.parseText(body) == xmlSlurper.parseText(mc.handlings[0].request.body as String)
    }

    def "should validate correct JSON"() {
        given:
        String body =
            """{
            |  "mapping": {
            |    "rules": [
            |      {
            |        "local": {
            |          "user": {
            |            "domain": "{D}",
            |            "name": "{D}",
            |            "email": "{D}",
            |            "expire": "{D}"
            |          }
            |        }
            |      }
            |    ],
            |    "version":"RAX-1"
            |  }
            |}
            |""".stripMargin()

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "PUT", headers: ["content-type": APPLICATION_JSON], requestBody: body)

        then:
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings.size() == 1
        jsonSlurper.parseText(body) == jsonSlurper.parseText(mc.handlings[0].request.body as String)
    }

    def "should validate correct YAML"() {
        given:
        String body =
            """---
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
            |""".stripMargin()

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "PUT", headers: ["content-type": TEXT_YAML], requestBody: body)

        then:
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings.size() == 1
        yaml.load(body) == yaml.load(mc.handlings[0].request.body as String)
    }

    def "should not remove the name attribute from a remote in a JSON policy"() {
        given:
        String body =
            """{
            |  "mapping": {
            |    "rules": [
            |      {
            |        "local": {
            |          "user": {
            |            "name": "{D}"
            |          }
            |        },
            |        "remote": [
            |          {
            |            "multiValue": false,
            |            "name": "Username",
            |            "regex": false
            |          }
            |        ]
            |      }
            |    ],
            |    "version":"RAX-1"
            |  }
            |}
            |""".stripMargin()

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "PUT", headers: ["content-type": APPLICATION_JSON], requestBody: body)

        then:
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings.size() == 1
        jsonSlurper.parseText(body) == jsonSlurper.parseText(mc.handlings[0].request.body as String)
    }

    def "should not remove the name attribute from a remote in a YAML policy"() {
        given:
        String body =
            """---
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
            |""".stripMargin()

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "PUT", headers: ["content-type": TEXT_YAML], requestBody: body)

        then:
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings.size() == 1
        yaml.load(body) == yaml.load(mc.handlings[0].request.body as String)
    }

    def "should not validate bad XML"() {
        given:
        String body =
            """<?xml version="1.0" encoding="UTF-8"?>
            |<mapping xmlns="http://docs.rackspace.com/identity/api/ext/MappingRules"
            |         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            |         xmlns:xs="http://www.w3.org/2001/XMLSchema"
            |         xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
            |         version="RAX-1">
            |   <rules>
            |      <rule>
            |         <local>
            |            <user>
            |               <name value="{D}"/>
            |               <email value="{D}"/>
            |               <expire value="{D}"/>
            |               <domain value="{D}"/>
            |               <roles value="{D}"/>
            |            </user>
            |         </local>
            |      </rule>
            |   </rules>
            |""".stripMargin()

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "PUT", headers: ["content-type": APPLICATION_XML], requestBody: body)

        then:
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST
        mc.handlings.size() == 0
    }

    def "should not validate bad JSON"() {
        given:
        String body =
            """{
            |  "mapping": {
            |    "rules": [
            |       {
            |        "local": {
            |          "user": {
            |            "domain":"{D}",
            |            "name":"{D}",
            |            "email":"{D}",
            |            "roles":"{D}",
            |            "expire":"{D}"
            |           }
            |         }
            |       }
            |     ],
            |    "version":"RAX-1"
            |  }
            |""".stripMargin()

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "PUT", headers: ["content-type": APPLICATION_JSON], requestBody: body)

        then:
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST
        mc.handlings.size() == 0
    }

    def "should fail to validate bad YAML"() {
        given:
        String body =
            """---
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
            |""".stripMargin()

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "PUT", headers: ["content-type": TEXT_YAML], requestBody: body)

        then:
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST
        mc.handlings.size() == 0
    }

    def "should fail to validate bad YAML (dimitry style)"() {
        given:
        String body =
            """- just: write some
            |- yaml:
            |  - [here, and]
            |  - {it: updates, in: real-time}
            |sdfsdds
            |""".stripMargin()

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "PUT", headers: ["content-type": TEXT_YAML], requestBody: body)

        then:
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST
        mc.handlings.size() == 0
    }

    def "YAML comments should be preserved"() {
        given:
        String comment = "Find me"
        String body =
            """---
            |mapping: # ${comment}
            |  rules:
            |  - local:
            |      user:
            |        name: "{D}"
            |    remote:
            |    - multiValue: false
            |      name: Username
            |      regex: false
            |  version: RAX-1
            |""".stripMargin()

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "PUT", headers: ["content-type": TEXT_YAML], requestBody: body)

        then:
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings.size() == 1
        (mc.handlings[0].request.body as String).contains(comment)
    }
}
