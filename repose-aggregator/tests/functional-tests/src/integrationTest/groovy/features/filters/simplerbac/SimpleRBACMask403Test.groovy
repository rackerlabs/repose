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
import scaffold.category.Filters
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.*

/**
 * Created by jennyvo on 6/1/15.
 */
@Category(Filters)
class SimpleRBACMask403Test extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/simplerbac", params)
        repose.configurationProvider.applyConfigs("features/filters/simplerbac/mask403", params)
        repose.start()
    }

    @Unroll("Test with #path, #method, #roles")
    def "Test simple RBAC with single role"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: ["X-Roles": roles])

        then:
        mc.receivedResponse.code as Integer == respcode

        where:
        path                 | method   | roles       | respcode
        "/path/to/this"      | "GET"    | "super"     | SC_OK
        "/path/to/this"      | "PUT"    | "super"     | SC_OK
        "/path/to/this"      | "POST"   | "super"     | SC_OK
        "/path/to/this"      | "DELETE" | "super"     | SC_OK
        "/path/to/this"      | "GET"    | "useradmin" | SC_OK
        "/path/to/this"      | "PUT"    | "useradmin" | SC_OK
        "/path/to/this"      | "POST"   | "useradmin" | SC_OK
        "/path/to/this"      | "DELETE" | "useradmin" | SC_METHOD_NOT_ALLOWED
        "/path/to/this"      | "GET"    | "admin"     | SC_OK
        "/path/to/this"      | "PUT"    | "admin"     | SC_OK
        "/path/to/this"      | "POST"   | "admin"     | SC_METHOD_NOT_ALLOWED
        "/path/to/this"      | "DELETE" | "admin"     | SC_METHOD_NOT_ALLOWED
        "/path/to/this"      | "GET"    | "user"      | SC_OK
        "/path/to/this"      | "PUT"    | "user"      | SC_METHOD_NOT_ALLOWED
        "/path/to/this"      | "POST"   | "user"      | SC_METHOD_NOT_ALLOWED
        "/path/to/this"      | "DELETE" | "user"      | SC_METHOD_NOT_ALLOWED
        "/path/to/this"      | "GET"    | "none"      | SC_NOT_FOUND
        "/path/to/this"      | "PUT"    | "none"      | SC_NOT_FOUND
        "/path/to/this"      | "POST"   | "none"      | SC_NOT_FOUND
        "/path/to/this"      | "DELETE" | "none"      | SC_NOT_FOUND
        "/path/to/that"      | "GET"    | "super"     | SC_OK
        "/path/to/that"      | "PUT"    | "super"     | SC_OK
        "/path/to/that"      | "POST"   | "super"     | SC_OK
        "/path/to/that"      | "DELETE" | "super"     | SC_OK
        "/path/to/that"      | "GET"    | "useradmin" | SC_OK
        "/path/to/that"      | "PUT"    | "useradmin" | SC_OK
        "/path/to/that"      | "POST"   | "useradmin" | SC_METHOD_NOT_ALLOWED
        "/path/to/that"      | "DELETE" | "useradmin" | SC_METHOD_NOT_ALLOWED
        "/path/to/test"      | "GET"    | "user"      | SC_OK
        "/path/to/test"      | "POST"   | "useradmin" | SC_OK
        "/path/to/test"      | "GET"    | "admin"     | SC_NOT_FOUND
        "/path/to/test"      | "POST"   | "super"     | SC_NOT_FOUND
        "/path/to/test"      | "PUT"    | "user"      | SC_METHOD_NOT_ALLOWED
        "/path/to/test"      | "DELETE" | "useradmin" | SC_METHOD_NOT_ALLOWED
        "/path/to/something" | "GET"    | "user"      | SC_NOT_FOUND
        "/path/to/something" | "GET"    | "super"     | SC_NOT_FOUND
        "/path/to/something" | "GET"    | "admin"     | SC_NOT_FOUND
        "/path/to/something" | "POST"   | "none"      | SC_NOT_FOUND
        "/path/to/something" | "PUT"    | "useradmin" | SC_NOT_FOUND
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
        "/path/to/that" | "POST"   | SC_METHOD_NOT_ALLOWED
        "/path/to/that" | "DELETE" | SC_METHOD_NOT_ALLOWED
    }

    @Unroll("Test with #path, #method, #roles")
    def "Test simple RBAC with multiple roles"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: ["X-Roles": roles])

        then:
        mc.receivedResponse.code as Integer == respcode

        where:
        path            | method   | roles              | respcode
        "/path/to/this" | "GET"    | "roleX,super,none" | SC_OK
        "/path/to/this" | "PUT"    | "roleX,super,none" | SC_OK
        "/path/to/this" | "POST"   | "roleX,super,none" | SC_OK
        "/path/to/this" | "DELETE" | "roleX,super,none" | SC_OK
        "/path/to/this" | "GET"    | "roleX,user,none"  | SC_OK
        "/path/to/this" | "PUT"    | "roleX,user,none"  | SC_METHOD_NOT_ALLOWED
        "/path/to/this" | "POST"   | "roleX,user,none"  | SC_METHOD_NOT_ALLOWED
        "/path/to/this" | "DELETE" | "roleX,user,none"  | SC_METHOD_NOT_ALLOWED
    }
}
