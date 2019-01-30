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
class SimpleRBACwParametersTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/simplerbac", params)
        repose.configurationProvider.applyConfigs("features/filters/simplerbac/parameterized", params)
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
        "/path/one/two/this" | "GET"    | "super"     | SC_OK
        "/path/one/two/this" | "PUT"    | "super"     | SC_OK
        "/path/one/two/this" | "POST"   | "super"     | SC_OK
        "/path/one/two/this" | "DELETE" | "super"     | SC_OK
        "/path/one/two/this" | "GET"    | "useradmin" | SC_OK
        "/path/one/two/this" | "PUT"    | "useradmin" | SC_OK
        "/path/one/two/this" | "POST"   | "useradmin" | SC_OK
        "/path/one/two/this" | "DELETE" | "useradmin" | SC_FORBIDDEN
        "/path/one/two/this" | "GET"    | "admin"     | SC_OK
        "/path/one/two/this" | "PUT"    | "admin"     | SC_OK
        "/path/one/two/this" | "POST"   | "admin"     | SC_FORBIDDEN
        "/path/one/two/this" | "DELETE" | "admin"     | SC_FORBIDDEN
        "/path/one/two/this" | "GET"    | "user"      | SC_OK
        "/path/one/two/this" | "PUT"    | "user"      | SC_FORBIDDEN
        "/path/one/two/this" | "POST"   | "user"      | SC_FORBIDDEN
        "/path/one/two/this" | "DELETE" | "user"      | SC_FORBIDDEN
        "/path/one/two/this" | "GET"    | "none"      | SC_FORBIDDEN
        "/path/one/two/this" | "PUT"    | "none"      | SC_FORBIDDEN
        "/path/one/two/this" | "POST"   | "none"      | SC_FORBIDDEN
        "/path/one/two/this" | "DELETE" | "none"      | SC_FORBIDDEN
        "/path/one/two/that" | "GET"    | "super"     | SC_OK
        "/path/one/two/that" | "PUT"    | "super"     | SC_OK
        "/path/one/two/that" | "POST"   | "super"     | SC_OK
        "/path/one/two/that" | "DELETE" | "super"     | SC_OK
        "/path/one/two/that" | "GET"    | "useradmin" | SC_OK
        "/path/one/two/that" | "PUT"    | "useradmin" | SC_OK
        "/path/one/two/that" | "POST"   | "user"      | SC_FORBIDDEN
        "/path/one/two/that" | "DELETE" | "admin"     | SC_FORBIDDEN
        "/path/one/two/that" | "POST"   | "super"     | SC_OK
        "/path/one/two/that" | "DELETE" | "super"     | SC_OK
        "/path/one/two/test" | "GET"    | "user"      | SC_OK
        "/path/one/two/test" | "POST"   | "useradmin" | SC_OK
        "/path/one/two/test" | "GET"    | "admin"     | SC_FORBIDDEN
        "/path/one/two/test" | "POST"   | "super"     | SC_FORBIDDEN
        "/path/one/two/test" | "PUT"    | "user"      | SC_METHOD_NOT_ALLOWED
        "/path/one/two/test" | "DELETE" | "useradmin" | SC_METHOD_NOT_ALLOWED
        "/path/to/something" | "GET"    | "user"      | SC_METHOD_NOT_ALLOWED
        "/path/to/something" | "GET"    | "super"     | SC_METHOD_NOT_ALLOWED
        "/path/to/something" | "GET"    | "admin"     | SC_METHOD_NOT_ALLOWED
        "/path/to/something" | "POST"   | "none"      | SC_METHOD_NOT_ALLOWED
        "/path/to/something" | "PUT"    | "useradmin" | SC_METHOD_NOT_ALLOWED
    }

    @Unroll("Test with #path, #method")
    def "Test simple RBAC w/o Roles"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method)

        then:
        mc.receivedResponse.code as Integer == respcode

        where:
        path                 | method   | respcode
        "/path/one/two/that" | "GET"    | SC_OK
        "/path/one/two/that" | "PUT"    | SC_OK
        "/path/one/two/that" | "POST"   | SC_FORBIDDEN
        "/path/one/two/that" | "DELETE" | SC_FORBIDDEN
    }

    @Unroll("Test with #path, #method, #roles")
    def "Test simple RBAC with multiple roles"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: ["X-Roles": roles])

        then:
        mc.receivedResponse.code as Integer == respcode

        where:
        path                 | method   | roles              | respcode
        "/path/one/two/this" | "GET"    | "roleX,super,none" | SC_OK
        "/path/one/two/this" | "PUT"    | "roleX,super,none" | SC_OK
        "/path/one/two/this" | "POST"   | "roleX,super,none" | SC_OK
        "/path/one/two/this" | "DELETE" | "roleX,super,none" | SC_OK
        "/path/one/two/this" | "GET"    | "roleX,user,none"  | SC_OK
        "/path/one/two/this" | "PUT"    | "roleX,user,none"  | SC_FORBIDDEN
        "/path/one/two/this" | "POST"   | "roleX,user,none"  | SC_FORBIDDEN
        "/path/one/two/this" | "DELETE" | "roleX,user,none"  | SC_FORBIDDEN
    }
}
