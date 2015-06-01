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

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 6/1/15.
 */
class SimpleRBACTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/simplerbac", params)
        repose.start()
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    @Unroll("Test with with #path, #method, #roles")
    def "Test simple RBAC" () {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: ["X-Roles": roles])

        then:
        mc.receivedResponse.code == respcode

        where:
        path                | method    | roles             | respcode
        "/service/test"     | "GET"     | "user"            | "200"
        "/service/abc"      | "GET"     | "useradmin"       | "200"
        "/service/test"     | "POST"    | "user"            | "403"
        "/service/test"     | "POST"    | "useradmin"       | "200"
        "/service/test"     | "PUT"     | "useradmin"       | "200"
        "/service/test"     | "DELETE"  | "useradmin"       | "200"
        "/service/test"     | "POST"    | "useradmin"       | "200"
        "/service/test"     | "POST"    | "useradmin"       | "200"
        "/service/test"     | "POST"    | "user,admin"      | "200"
        "/service/test"     | "DELETE"  | "user,useradmin"  | "200"
        "/test/one"         | "GET"     | "user"            | "200"
        "/test/two"         | "PUT"     | "user"            | "200"
        "/test/three"       | "GET"     | "useradmin"       | "200"
        "/test/four"        | "GET"     | "admin"           | "200"
        "/test/five"        | "PUT"     | "admin"           | "200"
        "/test/six"         | "POST"    | "admin"           | "405"
        "/server/test1"     | "GET"     | "user"            | "403"
        "/server/test2"     | "POST"    | "user"            | "403"
        "/server/test3"     | "PUT"     | "user"            | "403"
        "/server/test4"     | "DELETE"  | "user"            | "403"
        "/server/test5"     | "GET"     | "useradmin"       | "200"
        "/server/test6"     | "POST"    | "user,admin"      | "200"
        "/server/test7"     | "PUT"     | "user,useradmin"  | "200"
        "/server/test8"     | "DELETE"  | "admin"           | "200"
        "/custom/test"      | "GET"     | "admin"           | "405"
        "/custom/test"      | "POST"    | "useradmin"       | "405"
        "/custom/test"      | "PUT"     | "user"            | "405"
        "/custom/test"      | "DELETE"  | "admin"           | "405"
    }
}
