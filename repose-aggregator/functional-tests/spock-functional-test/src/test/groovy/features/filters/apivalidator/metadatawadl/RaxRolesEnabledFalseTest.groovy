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
package features.filters.apivalidator.metadatawadl

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 9/14/15.
 */
class RaxRolesEnabledFalseTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/metadataextension", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/metadataextension/raxroleenabledfalse", params)

        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    // Standard/Custom GET's, PUT's, DELETE's, and POST's are forbidden when RAX-Roles are not enabled.
    // Access should be forbidden(403).
    @Unroll("Method: #method, Path: #path, Roles: #headers, and respcode: 403")
    def "Test metadata extension when rax roles not enabled"() {
        given:
        MessageChain mc

        when:
        mc = deproxy.makeRequest(url: reposeEndpoint + "/" + path, method: method, headers: headers)

        then:
        mc.getReceivedResponse().getCode().equals("403")

        where:
        method    | path                       | headers
        "GET"     | "standard/foo"             | ["x-roles": "repose_test, admin"]
        "GET"     | "standard/billing:foo"     | ["x-roles": "repose_test, admin"]
        "GET"     | "standard/service:foo"     | ["x-roles": "repose_test, admin"]
        "GET"     | "custom/foo"               | ["x-roles": "repose_test, admin"]
        "GET"     | "custom/customer:role:foo" | ["x-roles": "repose_test, admin"]
        "GET"     | "custom/service:foo"       | ["x-roles": "repose_test, admin"]
        "GET"     | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, admin"]

        "GET"     | "standard/foo"             | ["x-roles": "repose_test, billing:role"]
        "GET"     | "standard/billing:foo"     | ["x-roles": "repose_test, billing:role"]
        "GET"     | "standard/service:foo"     | ["x-roles": "repose_test, billing:role"]
        "GET"     | "custom/foo"               | ["x-roles": "repose_test, billing:role"]
        "GET"     | "custom/customer:role:foo" | ["x-roles": "repose_test, billing:role"]
        "GET"     | "custom/service:foo"       | ["x-roles": "repose_test, billing:role"]
        "GET"     | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, billing:role"]

        "GET"     | "standard/foo"             | ["x-roles": "repose_test, service:role"]
        "GET"     | "standard/billing:foo"     | ["x-roles": "repose_test, service:role"]
        "GET"     | "standard/service:foo"     | ["x-roles": "repose_test, service:role"]
        "GET"     | "custom/foo"               | ["x-roles": "repose_test, service:role"]
        "GET"     | "custom/customer:role:foo" | ["x-roles": "repose_test, service:role"]
        "GET"     | "custom/service:foo"       | ["x-roles": "repose_test, service:role"]
        "GET"     | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, service:role"]

        "GET"     | "standard/foo"             | ["x-roles": "repose_test, superAdmin"]
        "GET"     | "standard/billing:foo"     | ["x-roles": "repose_test, superAdmin"]
        "GET"     | "standard/service:foo"     | ["x-roles": "repose_test, superAdmin"]
        "GET"     | "custom/foo"               | ["x-roles": "repose_test, superAdmin"]
        "GET"     | "custom/customer:role:foo" | ["x-roles": "repose_test, superAdmin"]
        "GET"     | "custom/service:foo"       | ["x-roles": "repose_test, superAdmin"]
        "GET"     | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, superAdmin"]

        "GET"     | "standard/foo"             | ["x-roles": "repose_test, customer:role"]
        "GET"     | "standard/billing:foo"     | ["x-roles": "repose_test, customer:role"]
        "GET"     | "standard/service:foo"     | ["x-roles": "repose_test, customer:role"]
        "GET"     | "custom/foo"               | ["x-roles": "repose_test, customer:role"]
        "GET"     | "custom/customer:role:foo" | ["x-roles": "repose_test, customer:role"]
        "GET"     | "custom/service:foo"       | ["x-roles": "repose_test, customer:role"]
        "GET"     | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, customer:role"]

        "GET"     | "standard/foo"             | ["x-roles": "repose_test, another_role"]
        "GET"     | "standard/billing:foo"     | ["x-roles": "repose_test, another_role"]
        "GET"     | "standard/service:foo"     | ["x-roles": "repose_test, another_role"]
        "GET"     | "custom/foo"               | ["x-roles": "repose_test, another_role"]
        "GET"     | "custom/customer:role:foo" | ["x-roles": "repose_test, another_role"]
        "GET"     | "custom/service:foo"       | ["x-roles": "repose_test, another_role"]
        "GET"     | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, another_role"]

        "GET"     | "standard/foo"             | ["x-roles": "repose_test, ???"]
        "GET"     | "standard/billing:foo"     | ["x-roles": "repose_test, ???"]
        "GET"     | "standard/service:foo"     | ["x-roles": "repose_test, ???"]
        "GET"     | "custom/foo"               | ["x-roles": "repose_test, ???"]
        "GET"     | "custom/customer:role:foo" | ["x-roles": "repose_test, ???"]
        "GET"     | "custom/service:foo"       | ["x-roles": "repose_test, ???"]
        "GET"     | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, ???"]

        "PUT"     | "standard/foo"             | ["x-roles": "repose_test, admin"]
        "PUT"     | "standard/billing:foo"     | ["x-roles": "repose_test, admin"]
        "PUT"     | "standard/service:foo"     | ["x-roles": "repose_test, admin"]
        "PUT"     | "custom/foo"               | ["x-roles": "repose_test, admin"]
        "PUT"     | "custom/customer:role:foo" | ["x-roles": "repose_test, admin"]
        "PUT"     | "custom/service:foo"       | ["x-roles": "repose_test, admin"]
        "PUT"     | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, admin"]

        "PUT"     | "standard/foo"             | ["x-roles": "repose_test, billing:role"]
        "PUT"     | "standard/billing:foo"     | ["x-roles": "repose_test, billing:role"]
        "PUT"     | "standard/service:foo"     | ["x-roles": "repose_test, billing:role"]
        "PUT"     | "custom/foo"               | ["x-roles": "repose_test, billing:role"]
        "PUT"     | "custom/customer:role:foo" | ["x-roles": "repose_test, billing:role"]
        "PUT"     | "custom/service:foo"       | ["x-roles": "repose_test, billing:role"]
        "PUT"     | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, billing:role"]

        "PUT"     | "standard/foo"             | ["x-roles": "repose_test, service:role"]
        "PUT"     | "standard/billing:foo"     | ["x-roles": "repose_test, service:role"]
        "PUT"     | "standard/service:foo"     | ["x-roles": "repose_test, service:role"]
        "PUT"     | "custom/foo"               | ["x-roles": "repose_test, service:role"]
        "PUT"     | "custom/customer:role:foo" | ["x-roles": "repose_test, service:role"]
        "PUT"     | "custom/service:foo"       | ["x-roles": "repose_test, service:role"]
        "PUT"     | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, service:role"]

        "PUT"     | "standard/foo"             | ["x-roles": "repose_test, superAdmin"]
        "PUT"     | "standard/billing:foo"     | ["x-roles": "repose_test, superAdmin"]
        "PUT"     | "standard/service:foo"     | ["x-roles": "repose_test, superAdmin"]
        "PUT"     | "custom/foo"               | ["x-roles": "repose_test, superAdmin"]
        "PUT"     | "custom/customer:role:foo" | ["x-roles": "repose_test, superAdmin"]
        "PUT"     | "custom/service:foo"       | ["x-roles": "repose_test, superAdmin"]
        "PUT"     | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, superAdmin"]

        "PUT"     | "standard/foo"             | ["x-roles": "repose_test, customer:role"]
        "PUT"     | "standard/billing:foo"     | ["x-roles": "repose_test, customer:role"]
        "PUT"     | "standard/service:foo"     | ["x-roles": "repose_test, customer:role"]
        "PUT"     | "custom/foo"               | ["x-roles": "repose_test, customer:role"]
        "PUT"     | "custom/customer:role:foo" | ["x-roles": "repose_test, customer:role"]
        "PUT"     | "custom/service:foo"       | ["x-roles": "repose_test, customer:role"]
        "PUT"     | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, customer:role"]

        "PUT"     | "standard/foo"             | ["x-roles": "repose_test, another_role"]
        "PUT"     | "standard/billing:foo"     | ["x-roles": "repose_test, another_role"]
        "PUT"     | "standard/service:foo"     | ["x-roles": "repose_test, another_role"]
        "PUT"     | "custom/foo"               | ["x-roles": "repose_test, another_role"]
        "PUT"     | "custom/customer:role:foo" | ["x-roles": "repose_test, another_role"]
        "PUT"     | "custom/service:foo"       | ["x-roles": "repose_test, another_role"]
        "PUT"     | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, another_role"]

        "PUT"     | "standard/foo"             | ["x-roles": "repose_test, ???"]
        "PUT"     | "standard/billing:foo"     | ["x-roles": "repose_test, ???"]
        "PUT"     | "standard/service:foo"     | ["x-roles": "repose_test, ???"]
        "PUT"     | "custom/foo"               | ["x-roles": "repose_test, ???"]
        "PUT"     | "custom/customer:role:foo" | ["x-roles": "repose_test, ???"]
        "PUT"     | "custom/service:foo"       | ["x-roles": "repose_test, ???"]
        "PUT"     | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, ???"]

        "DELETE"  | "standard/foo"             | ["x-roles": "repose_test, admin"]
        "DELETE"  | "standard/billing:foo"     | ["x-roles": "repose_test, admin"]
        "DELETE"  | "standard/service:foo"     | ["x-roles": "repose_test, admin"]
        "DELETE"  | "custom/foo"               | ["x-roles": "repose_test, admin"]
        "DELETE"  | "custom/customer:role:foo" | ["x-roles": "repose_test, admin"]
        "DELETE"  | "custom/service:foo"       | ["x-roles": "repose_test, admin"]
        "DELETE"  | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, admin"]

        "DELETE"  | "standard/foo"             | ["x-roles": "repose_test, billing:role"]
        "DELETE"  | "standard/billing:foo"     | ["x-roles": "repose_test, billing:role"]
        "DELETE"  | "standard/service:foo"     | ["x-roles": "repose_test, billing:role"]
        "DELETE"  | "custom/foo"               | ["x-roles": "repose_test, billing:role"]
        "DELETE"  | "custom/customer:role:foo" | ["x-roles": "repose_test, billing:role"]
        "DELETE"  | "custom/service:foo"       | ["x-roles": "repose_test, billing:role"]
        "DELETE"  | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, billing:role"]

        "DELETE"  | "standard/foo"             | ["x-roles": "repose_test, service:role"]
        "DELETE"  | "standard/billing:foo"     | ["x-roles": "repose_test, service:role"]
        "DELETE"  | "standard/service:foo"     | ["x-roles": "repose_test, service:role"]
        "DELETE"  | "custom/foo"               | ["x-roles": "repose_test, service:role"]
        "DELETE"  | "custom/customer:role:foo" | ["x-roles": "repose_test, service:role"]
        "DELETE"  | "custom/service:foo"       | ["x-roles": "repose_test, service:role"]
        "DELETE"  | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, service:role"]

        "DELETE"  | "standard/foo"             | ["x-roles": "repose_test, superAdmin"]
        "DELETE"  | "standard/billing:foo"     | ["x-roles": "repose_test, superAdmin"]
        "DELETE"  | "standard/service:foo"     | ["x-roles": "repose_test, superAdmin"]
        "DELETE"  | "custom/foo"               | ["x-roles": "repose_test, superAdmin"]
        "DELETE"  | "custom/customer:role:foo" | ["x-roles": "repose_test, superAdmin"]
        "DELETE"  | "custom/service:foo"       | ["x-roles": "repose_test, superAdmin"]
        "DELETE"  | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, superAdmin"]

        "DELETE"  | "standard/foo"             | ["x-roles": "repose_test, customer:role"]
        "DELETE"  | "standard/billing:foo"     | ["x-roles": "repose_test, customer:role"]
        "DELETE"  | "standard/service:foo"     | ["x-roles": "repose_test, customer:role"]
        "DELETE"  | "custom/foo"               | ["x-roles": "repose_test, customer:role"]
        "DELETE"  | "custom/customer:role:foo" | ["x-roles": "repose_test, customer:role"]
        "DELETE"  | "custom/service:foo"       | ["x-roles": "repose_test, customer:role"]
        "DELETE"  | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, customer:role"]

        "DELETE"  | "standard/foo"             | ["x-roles": "repose_test, another_role"]
        "DELETE"  | "standard/billing:foo"     | ["x-roles": "repose_test, another_role"]
        "DELETE"  | "standard/service:foo"     | ["x-roles": "repose_test, another_role"]
        "DELETE"  | "custom/foo"               | ["x-roles": "repose_test, another_role"]
        "DELETE"  | "custom/customer:role:foo" | ["x-roles": "repose_test, another_role"]
        "DELETE"  | "custom/service:foo"       | ["x-roles": "repose_test, another_role"]
        "DELETE"  | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, another_role"]

        "DELETE"  | "standard/foo"             | ["x-roles": "repose_test, ???"]
        "DELETE"  | "standard/billing:foo"     | ["x-roles": "repose_test, ???"]
        "DELETE"  | "standard/service:foo"     | ["x-roles": "repose_test, ???"]
        "DELETE"  | "custom/foo"               | ["x-roles": "repose_test, ???"]
        "DELETE"  | "custom/customer:role:foo" | ["x-roles": "repose_test, ???"]
        "DELETE"  | "custom/service:foo"       | ["x-roles": "repose_test, ???"]
        "DELETE"  | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, ???"]

        "POST"    | "standard/foo"             | ["x-roles": "repose_test, admin"]
        "POST"    | "standard/billing:foo"     | ["x-roles": "repose_test, admin"]
        "POST"    | "standard/service:foo"     | ["x-roles": "repose_test, admin"]
        "POST"    | "custom/foo"               | ["x-roles": "repose_test, admin"]
        "POST"    | "custom/customer:role:foo" | ["x-roles": "repose_test, admin"]
        "POST"    | "custom/service:foo"       | ["x-roles": "repose_test, admin"]
        "POST"    | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, admin"]

        "POST"    | "standard/foo"             | ["x-roles": "repose_test, billing:role"]
        "POST"    | "standard/billing:foo"     | ["x-roles": "repose_test, billing:role"]
        "POST"    | "standard/service:foo"     | ["x-roles": "repose_test, billing:role"]
        "POST"    | "custom/foo"               | ["x-roles": "repose_test, billing:role"]
        "POST"    | "custom/customer:role:foo" | ["x-roles": "repose_test, billing:role"]
        "POST"    | "custom/service:foo"       | ["x-roles": "repose_test, billing:role"]
        "POST"    | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, billing:role"]

        "POST"    | "standard/foo"             | ["x-roles": "repose_test, service:role"]
        "POST"    | "standard/billing:foo"     | ["x-roles": "repose_test, service:role"]
        "POST"    | "standard/service:foo"     | ["x-roles": "repose_test, service:role"]
        "POST"    | "custom/foo"               | ["x-roles": "repose_test, service:role"]
        "POST"    | "custom/customer:role:foo" | ["x-roles": "repose_test, service:role"]
        "POST"    | "custom/service:foo"       | ["x-roles": "repose_test, service:role"]
        "POST"    | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, service:role"]

        "POST"    | "standard/foo"             | ["x-roles": "repose_test, superAdmin"]
        "POST"    | "standard/billing:foo"     | ["x-roles": "repose_test, superAdmin"]
        "POST"    | "standard/service:foo"     | ["x-roles": "repose_test, superAdmin"]
        "POST"    | "custom/foo"               | ["x-roles": "repose_test, superAdmin"]
        "POST"    | "custom/customer:role:foo" | ["x-roles": "repose_test, superAdmin"]
        "POST"    | "custom/service:foo"       | ["x-roles": "repose_test, superAdmin"]
        "POST"    | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, superAdmin"]

        "POST"    | "standard/foo"             | ["x-roles": "repose_test, customer:role"]
        "POST"    | "standard/billing:foo"     | ["x-roles": "repose_test, customer:role"]
        "POST"    | "standard/service:foo"     | ["x-roles": "repose_test, customer:role"]
        "POST"    | "custom/foo"               | ["x-roles": "repose_test, customer:role"]
        "POST"    | "custom/customer:role:foo" | ["x-roles": "repose_test, customer:role"]
        "POST"    | "custom/service:foo"       | ["x-roles": "repose_test, customer:role"]
        "POST"    | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, customer:role"]

        "POST"    | "standard/foo"             | ["x-roles": "repose_test, another_role"]
        "POST"    | "standard/billing:foo"     | ["x-roles": "repose_test, another_role"]
        "POST"    | "standard/service:foo"     | ["x-roles": "repose_test, another_role"]
        "POST"    | "custom/foo"               | ["x-roles": "repose_test, another_role"]
        "POST"    | "custom/customer:role:foo" | ["x-roles": "repose_test, another_role"]
        "POST"    | "custom/service:foo"       | ["x-roles": "repose_test, another_role"]
        "POST"    | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, another_role"]

        "POST"    | "standard/foo"             | ["x-roles": "repose_test, ???"]
        "POST"    | "standard/billing:foo"     | ["x-roles": "repose_test, ???"]
        "POST"    | "standard/service:foo"     | ["x-roles": "repose_test, ???"]
        "POST"    | "custom/foo"               | ["x-roles": "repose_test, ???"]
        "POST"    | "custom/customer:role:foo" | ["x-roles": "repose_test, ???"]
        "POST"    | "custom/service:foo"       | ["x-roles": "repose_test, ???"]
        "POST"    | "custom/%7C%7C%7C:foo"     | ["x-roles": "repose_test, ???"]
    }
}
