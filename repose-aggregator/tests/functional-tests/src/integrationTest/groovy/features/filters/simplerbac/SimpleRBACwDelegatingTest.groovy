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

/**
 * Created by jennyvo on 6/2/15.
 */
@Category(Filters)
class SimpleRBACwDelegatingTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/simplerbac", params)
        repose.configurationProvider.applyConfigs("features/filters/simplerbac/delegating", params)
        repose.start()
    }

    @Unroll("Test with #path, #method, #roles")
    def "Test simple RBAC with single role"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: ["X-Roles": roles])

        then:
        mc.receivedResponse.code == "200"
        mc.handlings[0].request.headers.contains("X-Delegated")
        mc.handlings[0].request.headers.getFirstValue("X-Delegated") == delegateMsg

        where:
        path                 | method   | roles       | delegateMsg
        "/path/to/this"      | "DELETE" | "useradmin" | "status_code=403`component=simple-rbac`message=You are forbidden to perform the operation;q=0.5"
        "/path/to/this"      | "POST"   | "admin"     | "status_code=403`component=simple-rbac`message=You are forbidden to perform the operation;q=0.5"
        "/path/to/this"      | "DELETE" | "admin"     | "status_code=403`component=simple-rbac`message=You are forbidden to perform the operation;q=0.5"
        "/path/to/this"      | "PUT"    | "user"      | "status_code=403`component=simple-rbac`message=You are forbidden to perform the operation;q=0.5"
        "/path/to/this"      | "POST"   | "user"      | "status_code=403`component=simple-rbac`message=You are forbidden to perform the operation;q=0.5"
        "/path/to/this"      | "DELETE" | "user"      | "status_code=403`component=simple-rbac`message=You are forbidden to perform the operation;q=0.5"
        "/path/to/this"      | "GET"    | "none"      | "status_code=403`component=simple-rbac`message=You are forbidden to perform the operation;q=0.5"
        "/path/to/this"      | "PUT"    | "none"      | "status_code=403`component=simple-rbac`message=You are forbidden to perform the operation;q=0.5"
        "/path/to/this"      | "POST"   | "none"      | "status_code=403`component=simple-rbac`message=You are forbidden to perform the operation;q=0.5"
        "/path/to/this"      | "DELETE" | "none"      | "status_code=403`component=simple-rbac`message=You are forbidden to perform the operation;q=0.5"
        "/path/to/that"      | "POST"   | "user"      | "status_code=403`component=simple-rbac`message=You are forbidden to perform the operation;q=0.5"
        "/path/to/that"      | "DELETE" | "admin"     | "status_code=403`component=simple-rbac`message=You are forbidden to perform the operation;q=0.5"
        "/path/to/test"      | "GET"    | "admin"     | "status_code=403`component=simple-rbac`message=You are forbidden to perform the operation;q=0.5"
        "/path/to/test"      | "POST"   | "super"     | "status_code=403`component=simple-rbac`message=You are forbidden to perform the operation;q=0.5"
        "/path/to/test"      | "PUT"    | "user"      | "status_code=405`component=simple-rbac`message=Bad method: PUT. The Method does not match the pattern: 'GET|POST';q=0.5"
        "/path/to/test"      | "DELETE" | "useradmin" | "status_code=405`component=simple-rbac`message=Bad method: DELETE. The Method does not match the pattern: 'GET|POST';q=0.5"
        "/path/to/something" | "GET"    | "user"      | "status_code=404`component=simple-rbac`message=Resource not found: /path/to/{something}. The URI segment does not match the pattern: 'test|that|this';q=0.5"
        "/path/to/something" | "GET"    | "super"     | "status_code=404`component=simple-rbac`message=Resource not found: /path/to/{something}. The URI segment does not match the pattern: 'test|that|this';q=0.5"
        "/path/to/something" | "GET"    | "admin"     | "status_code=404`component=simple-rbac`message=Resource not found: /path/to/{something}. The URI segment does not match the pattern: 'test|that|this';q=0.5"
        "/path/to/something" | "POST"   | "none"      | "status_code=404`component=simple-rbac`message=Resource not found: /path/to/{something}. The URI segment does not match the pattern: 'test|that|this';q=0.5"
        "/path/to/something" | "PUT"    | "useradmin" | "status_code=404`component=simple-rbac`message=Resource not found: /path/to/{something}. The URI segment does not match the pattern: 'test|that|this';q=0.5"
    }
}
