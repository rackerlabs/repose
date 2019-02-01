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
package features.filters.apivalidator.metadataextension

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.XmlParsing
import spock.lang.Unroll

@Category(XmlParsing)
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

    // Standard/Custom GET's, PUT's, DELETE's, and POST's are forbidden when RAX-Roles are not enabled.
    // Access should be not found (404).
    @Unroll("Method: #method, Path: #path, Roles: #headers, and respcode: 404")
    def "Test metadata extension when rax roles not enabled"() {
        given:
        MessageChain mc

        when:
        mc = deproxy.makeRequest(url: reposeEndpoint + "/" + path, method: method, headers: headers)

        then:
        mc.getReceivedResponse().getCode().equals("404")

        where:
        [method, path, headers] << [["GET", "PUT", "POST", "DELETE"],
                                    ["standard/foo",
                                     "standard/billing:foo",
                                     "standard/service:foo",
                                     "custom/foo",
                                     "custom/customer:role:foo",
                                     "custom/service:foo",
                                     "custom/%7C%7C%7C:foo"],
                                    [["x-roles": "repose_test, admin"],
                                     ["x-roles": "repose_test, billing:role"],
                                     ["x-roles": "repose_test, service:role"],
                                     ["x-roles": "repose_test, superAdmin"],
                                     ["x-roles": "repose_test, customer:role"],
                                     ["x-roles": "repose_test, another_role"],
                                     ["x-roles": "repose_test, ???"]]].combinations()

    }
}
