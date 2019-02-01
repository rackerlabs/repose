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
package features.filters.regexrbac

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.*
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.ROLES
import static org.openrepose.commons.utils.http.PowerApiHeader.RELEVANT_ROLES

@Category(Filters)
class RegexRbacAccessTest extends ReposeValveTest {

    static String HEAD = "HEAD"
    static String GET = "GET"
    static String PUT = "PUT"
    static String POST = "POST"
    static String PATCH = "PATCH"
    static String DELETE = "DELETE"
    static String OPTIONS = "OPTIONS"
    static String[] HTTP_METHODS = [
        HEAD,
        GET,
        PUT,
        POST,
        PATCH,
        DELETE,
        OPTIONS
    ]

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/regexrbac/common", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll
    def "a request for #resource which matches a defined resource should succeed"() {
        when: "a request is made to Repose"
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint + resource,
            method: GET,
            headers: [(ROLES): "any"]
        )

        then: "the origin service responds with a 200"
        messageChain.handlings.size() == 1
        messageChain.receivedResponse.code as Integer == SC_OK

        where:
        resource << ["/url/caSing", "/url/regex/1234", "/url/regex/9999", "/url/regex/foo"]
    }

    @Unroll
    def "a request for #resource which does not match any defined resource should be rejected"() {
        when: "a request is made to Repose"
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint + resource,
            method: GET,
            headers: [(ROLES): "any"]
        )

        then: "Repose responds with a 404"
        messageChain.handlings.size() == 0
        messageChain.receivedResponse.code as Integer == SC_NOT_FOUND

        where:
        resource << ["/url/casing", "/url/regex/12345", "/not/a/path"]
    }

    @Unroll
    def "a request with #roles for #resource should succeed"() {
        when: "a request is made to Repose"
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint + resource,
            method: GET,
            headers: roles ? [(ROLES): roles] : []
        )

        then: "the origin service responds with a 200"
        messageChain.handlings.size() == 1
        messageChain.receivedResponse.code as Integer == SC_OK

        where:
        resource                    | roles
        "/all"                      | null
        "/any"                      | null
        "/all"                      | "role1"
        "/all"                      | "9elor"
        "/any"                      | "role1"
        "/any"                      | "9elor"
        "/all"                      | "role1, role2"
        "/any"                      | "role1, role2"
        "/all/any"                  | "role1"
        "/any/all"                  | "role1"
        "/roles/simple"             | "role1"
        "/roles/simple"             | "role2"
        "/roles/simple"             | "role1,role2"
        "/roles/simple"             | "role1, role2"
        "/roles/nbsp"               | "nb sp"
        "/roles/nbsp/end"           | "nbsp "
        "/roles/casing"             | "ROLE1"
        "/multimatch/one"           | "wildcard, multimatch"
        "/multimatch/one/two/three" | "wildcard, multimatch"
        "/inheritance/parent/child" | "child"
        "/spaces"                   | "spaces"
    }

    @Unroll
    def "a request with #roles for #resource should be rejected"() {
        when: "a request is made to Repose"
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint + resource,
            method: GET,
            headers: [(ROLES): roles]
        )

        then: "Repose responds with a 403"
        messageChain.handlings.size() == 0
        messageChain.receivedResponse.code as Integer == SC_FORBIDDEN

        where:
        resource                    | roles
        "/roles/simple"             | "forbidden-role"
        "/roles/casing"             | "role1"
        "/roles/nbsp"               | "nb sp"
        "/roles/nbsp/end"           | "nbsp "
        "/multimatch/one"           | "multimatch"
        "/inheritance/parent/child" | "parent"
    }

    @Unroll
    def "a request with roles #userRoles is forwarded with relevant roles #relevantRoles"() {
        when: "a request is made to Repose"
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint + "/roles/simple",
            method: GET,
            headers: [(ROLES): userRoles]
        )

        then: "the origin service responds with a 200"
        messageChain.handlings.size() == 1
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "relevant roles are forwarded in a header"
        messageChain.handlings[0].request.headers.contains(RELEVANT_ROLES)
        messageChain.handlings[0].request.headers.findAll(RELEVANT_ROLES).head() == relevantRoles

        where:
        userRoles             || relevantRoles
        "role1"               || "role1"
        "role1, role2"        || "role1, role2"
        "role1, role2, role3" || "role1, role2"
    }

    @Unroll
    def "a request with method #method for a resource #resource allowing ANY or ALL methods should succeed"() {
        when: "a request is made to Repose"
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint + resource,
            method: method
        )

        then: "the origin service responds with a 200"
        messageChain.handlings.size() == 1
        messageChain.receivedResponse.code as Integer == SC_OK

        where:
        [resource, method] << [["/any", "/all", "/all/any", "/any/all"], HTTP_METHODS].combinations()
    }

    @Unroll
    def "a request with method #method for a resource allowing that method should succeed"() {
        when: "a request is made to Repose"
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint + "/method/${method.toString().toLowerCase()}",
            method: method
        )

        then: "the origin service responds with a 200"
        messageChain.handlings.size() == 1
        messageChain.receivedResponse.code as Integer == SC_OK

        where:
        method << HTTP_METHODS
    }

    @Unroll
    def "a request with method #method for a resource #resource which does not allow that method should be rejected"() {
        when: "a request is made to Repose"
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint + "/method/${resource.toString().toLowerCase()}",
            method: method
        )

        then: "Repose responds with a 403"
        messageChain.handlings.size() == 0
        messageChain.receivedResponse.code as Integer == SC_METHOD_NOT_ALLOWED

        where:
        [resource, method] << HTTP_METHODS.collect { outer -> (HTTP_METHODS - outer).collect { inner -> [outer, inner] } }.flatten().collate(2)
    }
}
