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
package features.filters.apivalidator

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.XmlParsing
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.*

@Category(XmlParsing)
class RelevantRolesTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/relevantroles", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll("method=#method,subResource=#subResource,headers=#headers ; expected relevant=#relevantRoles,response=#responseCode")
    def "when Relevant Roles is enabled, validate with wadl, and expect the Relevant Roles"() {
        given: "a configured and running Repose"

        when: "a request is made"
        def mc = deproxy.makeRequest(url: "$reposeEndpoint/$subResource/a", method: method, headers: headers)

        then: "the response should be correct"
        mc.receivedResponse.code as Integer == responseCode

        and: "only the correct Relevant Roles should have made it to the origin service OR the call was rejected appropriately"
        if (responseCode == SC_OK) {
            assert mc.handlings.size() == 1
            def relevantRolesRequestHeaders = mc.handlings[0].request.getHeaders().findAll("X-Relevant-Roles")
            assert relevantRolesRequestHeaders.containsAll(relevantRoles)
            assert relevantRoles.containsAll(relevantRolesRequestHeaders)
        } else {
            assert mc.handlings.size() == 0
        }

        where:
        method   | subResource        | headers                                       | relevantRoles             | responseCode
        "GET"    | "raxRolesEnabled"  | ["x-roles": ""]                               | null                      | SC_FORBIDDEN
        "GET"    | "raxRolesEnabled"  | ["x-roles": "a:admin"]                        | ["a:admin"]               | SC_OK
        "GET"    | "raxRolesEnabled"  | ["x-roles": "a:creator"]                      | null                      | SC_FORBIDDEN
        "GET"    | "raxRolesEnabled"  | ["x-roles": "a:observer"]                     | ["a:observer"]            | SC_OK
        "GET"    | "raxRolesEnabled"  | ["x-roles": "a:observer, a:creator"]          | ["a:observer"]            | SC_OK
        "GET"    | "raxRolesEnabled"  | ["x-roles": "a:creator, a:admin"]             | ["a:admin"]               | SC_OK
        "GET"    | "raxRolesEnabled"  | ["x-roles": "a:admin, a:creator, a:observer"] | ["a:admin", "a:observer"] | SC_OK
        "GET"    | "raxRolesEnabled"  | ["x-roles": "a:creator, a:admin, a:observer"] | ["a:admin", "a:observer"] | SC_OK
        "GET"    | "raxRolesEnabled"  | ["x-roles": "a:creator, a:observer, a:admin"] | ["a:admin", "a:observer"] | SC_OK
        "GET"    | "raxRolesEnabled"  | ["x-roles": "a:admin, a:creator, a:observer"] | ["a:observer", "a:admin"] | SC_OK
        "GET"    | "raxRolesEnabled"  | ["x-roles": "a:creator, a:admin, a:observer"] | ["a:observer", "a:admin"] | SC_OK
        "GET"    | "raxRolesEnabled"  | ["x-roles": "a:creator, a:observer, a:admin"] | ["a:observer", "a:admin"] | SC_OK
        "GET"    | "role-with-dashes" | ["x-roles": "a:admin-dash"]                   | ["a:admin-dash"]          | SC_OK
        "GET"    | "role-with-dashes" | ["x-roles": "This-Is-A-Role"]                 | ["This-Is-A-Role"]        | SC_OK
        "GET"    | "role-with-dashes" | ["x-roles": "a:admin"]                        | null                      | SC_FORBIDDEN
        "GET"    | "role-with-dashes" | ["x-roles": "a:admin,-dash"]                  | null                      | SC_FORBIDDEN
        "GET"    | "role-with-dashes" | ["x-roles": "This,Is,A,Role"]                 | null                      | SC_FORBIDDEN
        "GET"    | "role-with-spaces" | ["x-roles": "a:admin wsp"]                    | ["a:admin wsp"]           | SC_OK
        "GET"    | "role-with-spaces" | ["x-roles": "This Is A Role"]                 | ["This Is A Role"]        | SC_OK
        "GET"    | "role-with-spaces" | ["x-roles": "a:admin"]                        | null                      | SC_FORBIDDEN
        "GET"    | "role-with-spaces" | ["x-roles": "a:admin, wsp"]                   | null                      | SC_FORBIDDEN
        "GET"    | "role-with-spaces" | ["x-roles": "This,Is,A,Role"]                 | null                      | SC_FORBIDDEN
        "GET"    | "role-with-spaces" | ["x-roles": "This Is A&#xA0;Role"]            | null                      | SC_FORBIDDEN
        "PUT"    | "raxRolesEnabled"  | ["x-roles": ""]                               | null                      | SC_METHOD_NOT_ALLOWED
        "PUT"    | "raxRolesEnabled"  | ["x-roles": "a:admin"]                        | null                      | SC_METHOD_NOT_ALLOWED
        "PUT"    | "raxRolesEnabled"  | ["x-roles": "a:creator"]                      | null                      | SC_METHOD_NOT_ALLOWED
        "PUT"    | "raxRolesEnabled"  | ["x-roles": "a:observer"]                     | null                      | SC_METHOD_NOT_ALLOWED
        "PUT"    | "role-with-dashes" | ["x-roles": "a:admin-dash"]                   | null                      | SC_METHOD_NOT_ALLOWED
        "PUT"    | "role-with-dashes" | ["x-roles": "This-Is-A-Role"]                 | null                      | SC_METHOD_NOT_ALLOWED
        "PUT"    | "role-with-spaces" | ["x-roles": "a:admin wsp"]                    | null                      | SC_METHOD_NOT_ALLOWED
        "PUT"    | "role-with-spaces" | ["x-roles": "This Is A Role"]                 | null                      | SC_METHOD_NOT_ALLOWED
        "POST"   | "raxRolesEnabled"  | ["x-roles": ""]                               | null                      | SC_FORBIDDEN
        "POST"   | "raxRolesEnabled"  | ["x-roles": "a:admin"]                        | ["a:admin"]               | SC_OK
        "POST"   | "raxRolesEnabled"  | ["x-roles": "a:creator"]                      | null                      | SC_FORBIDDEN
        "POST"   | "raxRolesEnabled"  | ["x-roles": "a:observer"]                     | null                      | SC_FORBIDDEN
        "POST"   | "raxRolesEnabled"  | ["x-roles": "a:observer, a:creator"]          | null                      | SC_FORBIDDEN
        "POST"   | "raxRolesEnabled"  | ["x-roles": "a:creator, a:admin"]             | ["a:admin"]               | SC_OK
        "POST"   | "raxRolesEnabled"  | ["x-roles": "a:admin, a:creator, a:observer"] | ["a:admin"]               | SC_OK
        "POST"   | "raxRolesEnabled"  | ["x-roles": "a:creator, a:admin, a:observer"] | ["a:admin"]               | SC_OK
        "POST"   | "raxRolesEnabled"  | ["x-roles": "a:creator, a:observer, a:admin"] | ["a:admin"]               | SC_OK
        "POST"   | "role-with-dashes" | ["x-roles": "a:admin-dash"]                   | ["a:admin-dash"]          | SC_OK
        "POST"   | "role-with-dashes" | ["x-roles": "This-Is-A-Role"]                 | null                      | SC_FORBIDDEN
        "POST"   | "role-with-dashes" | ["x-roles": "a:admin"]                        | null                      | SC_FORBIDDEN
        "POST"   | "role-with-dashes" | ["x-roles": "a:admin,-dash"]                  | null                      | SC_FORBIDDEN
        "POST"   | "role-with-dashes" | ["x-roles": "This,Is,A,Role"]                 | null                      | SC_FORBIDDEN
        "POST"   | "role-with-spaces" | ["x-roles": "a:admin wsp"]                    | ["a:admin wsp"]           | SC_OK
        "POST"   | "role-with-spaces" | ["x-roles": "This Is A Role"]                 | null                      | SC_FORBIDDEN
        "POST"   | "role-with-spaces" | ["x-roles": "a:admin"]                        | null                      | SC_FORBIDDEN
        "POST"   | "role-with-spaces" | ["x-roles": "a:admin, wsp"]                   | null                      | SC_FORBIDDEN
        "POST"   | "role-with-spaces" | ["x-roles": "This,Is,A,Role"]                 | null                      | SC_FORBIDDEN
        "POST"   | "role-with-spaces" | ["x-roles": "This Is A&#xA0;Role"]            | null                      | SC_FORBIDDEN
        "DELETE" | "raxRolesEnabled"  | ["x-roles": ""]                               | null                      | SC_FORBIDDEN
        "DELETE" | "raxRolesEnabled"  | ["x-roles": "a:admin"]                        | ["a:admin"]               | SC_OK
        "DELETE" | "raxRolesEnabled"  | ["x-roles": "a:creator"]                      | null                      | SC_FORBIDDEN
        "DELETE" | "raxRolesEnabled"  | ["x-roles": "a:observer"]                     | null                      | SC_FORBIDDEN
        "DELETE" | "raxRolesEnabled"  | ["x-roles": "a:observer, a:creator"]          | null                      | SC_FORBIDDEN
        "DELETE" | "raxRolesEnabled"  | ["x-roles": "a:creator, a:admin"]             | ["a:admin"]               | SC_OK
        "DELETE" | "raxRolesEnabled"  | ["x-roles": "a:admin, a:creator, a:observer"] | ["a:admin"]               | SC_OK
        "DELETE" | "raxRolesEnabled"  | ["x-roles": "a:creator, a:admin, a:observer"] | ["a:admin"]               | SC_OK
        "DELETE" | "raxRolesEnabled"  | ["x-roles": "a:creator, a:observer, a:admin"] | ["a:admin"]               | SC_OK
        "DELETE" | "role-with-dashes" | ["x-roles": "a:admin-dash"]                   | ["a:admin-dash"]          | SC_OK
        "DELETE" | "role-with-dashes" | ["x-roles": "This-Is-A-Role"]                 | null                      | SC_FORBIDDEN
        "DELETE" | "role-with-dashes" | ["x-roles": "a:admin"]                        | null                      | SC_FORBIDDEN
        "DELETE" | "role-with-dashes" | ["x-roles": "a:admin,-dash"]                  | null                      | SC_FORBIDDEN
        "DELETE" | "role-with-dashes" | ["x-roles": "This,Is,A,Role"]                 | null                      | SC_FORBIDDEN
        "DELETE" | "role-with-spaces" | ["x-roles": "a:admin wsp"]                    | ["a:admin wsp"]           | SC_OK
        "DELETE" | "role-with-spaces" | ["x-roles": "This Is A Role"]                 | null                      | SC_FORBIDDEN
        "DELETE" | "role-with-spaces" | ["x-roles": "a:admin"]                        | null                      | SC_FORBIDDEN
        "DELETE" | "role-with-spaces" | ["x-roles": "a:admin, wsp"]                   | null                      | SC_FORBIDDEN
        "DELETE" | "role-with-spaces" | ["x-roles": "This,Is,A,Role"]                 | null                      | SC_FORBIDDEN
        "DELETE" | "role-with-spaces" | ["x-roles": "This Is A&#xA0;Role"]            | null                      | SC_FORBIDDEN
    }
}
