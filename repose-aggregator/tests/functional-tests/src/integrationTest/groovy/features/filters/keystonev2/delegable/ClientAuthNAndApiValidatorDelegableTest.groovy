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
package features.filters.keystonev2.delegable

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Unroll

/**
 * Created by jennyvo on 11/10/14.
 */
@Category(Filters)
class ClientAuthNAndApiValidatorDelegableTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/delegable", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/delegable/withapivalidator", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
    }

    /*
        This test to verify the forward fail reason and default quality for authn
    */
    @Unroll("req method: #method, #path, #apiDelegatedMsg")
    def "when req without token, non tenanted and delegable mode with quality"() {
        given:
        Map<String, String> headers = ["X-Roles"     : roles,
                                       "Content-Type": "application/xml"]
        def authDelegatedMsg = 'status_code=401.component=keystone-v2.message=X-Auth-Token header not found;q=0.3'

        when: "User passes a request through repose with authN and apiValidator delegable"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/$path",
                method: method,
                headers: headers)

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.contains("x-identity-status")
        request2.headers.getFirstValue("x-identity-status") == identityStatus
        request2.headers.findAll("x-delegated").size() == 2
        msgCheckingHelper(request2.headers.findAll("x-delegated"), authDelegatedMsg, apiDelegatedMsg)

        where:
        method   | path           | roles                 | identityStatus  | apiDelegatedMsg
        "GET"    | "servers/"     | "raxRole"             | "Indeterminate" | "status_code=403.component=api-validator.message=You are forbidden to perform the operation;q=0.6"
        "POST"   | "servers/1235" | "raxRole, a:observer" | "Indeterminate" | "status_code=404.component=api-validator.message=Resource not found:\\s/servers/.*;q=0.6"
        "PUT"    | "servers/"     | "raxRole, a:admin"    | "Indeterminate" | "status_code=404.component=api-validator.message=Bad method: PUT. The Method does not match the pattern: 'DELETE|GET|POST';q=0.6"
        "DELETE" | "servers/test" | "raxRole, a:observer" | "Indeterminate" | "status_code=404.component=api-validator.message=Resource not found:\\s/servers/.*;q=0.6"
        "GET"    | "get/"         | "raxRole"             | "Indeterminate" | "status_code=404.component=api-validator.message=Resource not found:\\s/.*;q=0.6"

    }

    def void msgCheckingHelper(List delegatingmsgs, String authmsg, String apimsg) {
        for (int i = 0; i < delegatingmsgs.size(); i++) {
            if (delegatingmsgs.get(i).toString().contains("api-validator")) {
                assert delegatingmsgs.get(i) =~ apimsg
            } else {
                assert delegatingmsgs.get(i) =~ authmsg
            }
        }
    }
}
