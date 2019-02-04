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
package features.filters.simplerbac

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.XmlParsing
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.*

// 200
// 403

/**
 * Created by jennyvo on 6/1/15.
 */
@Category(XmlParsing)
class SimpleRBACTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/simplerbac", params)
        repose.configurationProvider.applyConfigs("features/filters/simplerbac/notmasked", params)
        repose.start()
    }

    @Unroll("Test with #path, #method, #roles")
    def "Test simple RBAC with single role"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: ["X-Roles": roles])

        then:
        mc.receivedResponse.code as Integer == respcode

        where:
        path                 | method   | roles           | respcode
        "/path/to/this"      | "GET"    | "super"         | SC_OK
        "/path/to/this"      | "PUT"    | "super"         | SC_OK
        "/path/to/this"      | "POST"   | "super"         | SC_OK
        "/path/to/this"      | "DELETE" | "super"         | SC_OK
        "/path/to/this"      | "GET"    | "useradmin"     | SC_OK
        "/path/to/this"      | "PUT"    | "useradmin"     | SC_OK
        "/path/to/this"      | "POST"   | "useradmin"     | SC_OK
        "/path/to/this"      | "DELETE" | "useradmin"     | SC_FORBIDDEN
        "/path/to/this"      | "GET"    | "admin"         | SC_OK
        "/path/to/this"      | "PUT"    | "admin"         | SC_OK
        "/path/to/this"      | "POST"   | "admin"         | SC_FORBIDDEN
        "/path/to/this"      | "DELETE" | "admin"         | SC_FORBIDDEN
        "/path/to/this"      | "GET"    | "user"          | SC_OK
        "/path/to/this"      | "PUT"    | "user"          | SC_FORBIDDEN
        "/path/to/this"      | "POST"   | "user"          | SC_FORBIDDEN
        "/path/to/this"      | "DELETE" | "user"          | SC_FORBIDDEN
        "/path/to/this"      | "GET"    | "none"          | SC_FORBIDDEN
        "/path/to/this"      | "PUT"    | "none"          | SC_FORBIDDEN
        "/path/to/this"      | "POST"   | "none"          | SC_FORBIDDEN
        "/path/to/this"      | "DELETE" | "none"          | SC_FORBIDDEN
        "/path/to/that"      | "GET"    | "super"         | SC_OK
        "/path/to/that"      | "PUT"    | "super"         | SC_OK
        "/path/to/that"      | "POST"   | "super"         | SC_OK
        "/path/to/that"      | "DELETE" | "super"         | SC_OK
        "/path/to/that"      | "GET"    | "useradmin"     | SC_OK
        "/path/to/that"      | "PUT"    | "useradmin"     | SC_OK
        "/path/to/that"      | "POST"   | "user"          | SC_FORBIDDEN
        "/path/to/that"      | "DELETE" | "admin"         | SC_FORBIDDEN
        "/path/to/that"      | "POST"   | "super"         | SC_OK
        "/path/to/that"      | "DELETE" | "super"         | SC_OK
        "/path/to/test"      | "GET"    | "user"          | SC_OK
        "/path/to/test"      | "POST"   | "useradmin"     | SC_OK
        "/path/to/test"      | "GET"    | "admin"         | SC_FORBIDDEN
        "/path/to/test"      | "POST"   | "super"         | SC_FORBIDDEN
        "/path/to/test"      | "PUT"    | "user"          | SC_METHOD_NOT_ALLOWED
        "/path/to/test"      | "DELETE" | "useradmin"     | SC_METHOD_NOT_ALLOWED
        "/path/to/something" | "GET"    | "user"          | SC_NOT_FOUND
        "/path/to/something" | "GET"    | "super"         | SC_NOT_FOUND
        "/path/to/something" | "GET"    | "admin"         | SC_NOT_FOUND
        "/path/to/something" | "POST"   | "none"          | SC_NOT_FOUND
        "/path/to/something" | "PUT"    | "useradmin"     | SC_NOT_FOUND
        "/path/to/space"     | "GET"    | "super wsp"     | SC_OK
        "/path/to/space"     | "GET"    | "useradmin wsp" | SC_OK
        "/path/to/space"     | "GET"    | "admin wsp"     | SC_OK
        "/path/to/space"     | "GET"    | "super"         | SC_FORBIDDEN
        "/path/to/space"     | "GET"    | "useradmin"     | SC_FORBIDDEN
        "/path/to/space"     | "GET"    | "admin"         | SC_FORBIDDEN
        "/path/to/space"     | "GET"    | "wsp"           | SC_FORBIDDEN
    }

    @Unroll("Test with #path, #method")
    def "Test simple RBAC w/o Roles"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method)

        then:
        mc.receivedResponse.code as Integer == respcode

        where:
        path            | method   | respcode
        "/path/to/that" | "GET"    | SC_OK
        "/path/to/that" | "PUT"    | SC_OK
        "/path/to/that" | "POST"   | SC_FORBIDDEN
        "/path/to/that" | "DELETE" | SC_FORBIDDEN
    }

    @Unroll("Test with #path, #method, #roles")
    def "Test simple RBAC with multiple roles"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: ["X-Roles": roles])

        then:
        mc.receivedResponse.code as Integer == respcode

        and: "only the correct Relevant Roles should have made it to the origin service OR the call was rejected appropriately"
        if (respcode == SC_OK) {
            assert mc.handlings.size() == 1
            def relevantRolesRequestHeaders = mc.handlings[0].request.getHeaders().findAll("X-Relevant-Roles")
            assert relevantRolesRequestHeaders.containsAll(relevantRoles)
            assert relevantRoles.containsAll(relevantRolesRequestHeaders)
        } else {
            assert mc.handlings.size() == 0
        }

        where:
        path            | method   | roles                    | relevantRoles      | respcode
        "/path/to/this" | "GET"    | "roleX,super,none,admin" | ["super", "admin"] | SC_OK
        "/path/to/this" | "PUT"    | "roleX,super,none,admin" | ["super", "admin"] | SC_OK
        "/path/to/this" | "POST"   | "roleX,super,none,admin" | ["super"]          | SC_OK
        "/path/to/this" | "DELETE" | "roleX,super,none,admin" | ["super"]          | SC_OK
        "/path/to/this" | "GET"    | "roleX,user,none,admin"  | ["admin", "user"]  | SC_OK
        "/path/to/this" | "PUT"    | "roleX,user,none,admin"  | ["admin"]          | SC_OK
        "/path/to/this" | "POST"   | "roleX,user,none,admin"  | null               | SC_FORBIDDEN
        "/path/to/this" | "DELETE" | "roleX,user,none,admin"  | null               | SC_FORBIDDEN
    }
}
