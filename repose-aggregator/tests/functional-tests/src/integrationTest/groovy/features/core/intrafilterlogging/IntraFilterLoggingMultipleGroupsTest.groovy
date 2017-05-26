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
package features.core.intrafilterlogging

import groovy.json.JsonSlurper
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

/**
 * Created by jennyvo on 10/5/15.
 *  Previously intrafilter logging at trace level only log the first group
 *      from the list and ignore the rest of groups
 *  Fix log all groups (x-pp-groups)
 */
class IntraFilterLoggingMultipleGroupsTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {

        deproxy = new Deproxy()
        reposeLogSearch.cleanLog()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/intrafilterlogging/tracingmultigroups", params)

        originEndpoint = deproxy.addEndpoint(params.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(params.identityPort, params.targetPort)
        identityEndpoint = deproxy.addEndpoint(params.identityPort,
                'identity service', null, fakeIdentityV2Service.handler)

        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def setup() {
        fakeIdentityV2Service.resetDefaultParameters()
    }

    def "Verify log all groups to x-pp-groups from translation filter going into echo filter"() {
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = "mytenant"
            client_tenantname = "mytenantname"
        }

        def headers = [
                'content-type': 'application/json',
                'X-Auth-Token': fakeIdentityV2Service.client_token,
                'x-roles'     : 'test',
                'X-Roles'     : 'user',
                "x-pp-groups" : 'Repose_test_group'
        ]

        when: "User passes a request through repose with valid token"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/test", method: 'GET',
                headers: headers)

        //We want to make sure that the incoming bits to the echo filter show all the users that were translated
        String tracelogLine = reposeLogSearch.searchByString('TRACE intrafilter-logging - ."preamble":"Intrafilter Request Log","timestamp":(.*),"currentFilter":"echo')
        String jsonpart = tracelogLine.substring(tracelogLine.indexOf("{"))
        println(jsonpart)
        def slurper = new JsonSlurper()
        def logresult = slurper.parseText(jsonpart)

        then: "They should pass"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 0 //Hitting the echo filter instead
        //No need to assert that repose didn't split filters, we've got many tests to prove that

        //intrafilterlogging check after headers translation
        logresult.headers["x-roles"].toString().contains("test")
        logresult.headers["x-roles"].toString().contains("user")
        logresult.headers["x-roles"].toString().contains("compute:admin")
        logresult.headers["x-roles"].toString().contains("object-store:admin")
        logresult.headers["x-roles"].toString().contains("service:admin-role1")
        logresult.headers["x-pp-groups"].toString().contains("Repose_test_group")
        logresult.headers["x-pp-groups"].toString().contains("0")
        logresult.headers["x-pp-groups"].toString().contains("compute:admin")
        logresult.headers["x-pp-groups"].toString().contains("object-store:admin")
        logresult.headers["x-pp-groups"].toString().contains("service:admin-role1")
    }
}
