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
package features.filters.herp

import groovy.json.JsonSlurper
import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV3Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters

/**
 * Created by jennyvo on 1/12/15.
 */
@Category(Filters)
class HerpWithIdentityV3Test extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs('features/filters/herp', params)
        repose.configurationProvider.applyConfigs('features/filters/herp/withIdentityV3', params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        fakeIdentityV3Service.resetCounts()
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)
    }


    def "When using herp filter with identity V3 filter the set of headers include projectId will be added to log"() {
        given:
        List listattr = ["GUI", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message"]
        reposeLogSearch.cleanLog()

        when: "I send a GET request to Repose with an X-Subject-Token header"
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            default_region = "DFW"
            client_userid = 12345
            impersonate_name = "impersonateuser"
            impersonate_id = "567"
        }
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Subject-Token': fakeIdentityV3Service.client_token])

        String logLine = reposeLogSearch.searchByString("INFO  highly-efficient-record-processor")
        String jsonpart = logLine.substring(logLine.indexOf("{"))
        def slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)

        then:
        mc.receivedResponse.code == "200"
        checkAttribute(jsonpart, listattr)
        result.ServiceCode == "repose"
        result.Region == "USA"
        result.DataCenter == "DFW"
        result.Request.Method == "GET"
        result.Request.ProjectID[0] == null
        result.Request.UserName == "username"
        result.Request.ImpersonatorName == fakeIdentityV3Service.impersonate_name
        result.Response.Code == 200
        result.Response.Message == "OK"
    }

    def "when client failed to authenticate, the auth filter failed before get to herp"() {
        given:
        List listattr = ["GUI", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message"]
        reposeLogSearch.cleanLog()
        fakeIdentityV3Service.with {
            client_domainid = 11111
            client_userid = 11111
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        fakeIdentityV3Service.validateTokenHandler = {
            tokenId, request ->
                new Response(404, null, null, fakeIdentityV3Service.identityFailureAuthJsonRespTemplate)
        }


        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/11111/",
                method: 'GET',
                headers: [
                        'content-type'   : 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "401"
        mc.receivedResponse.headers.getFirstValue("WWW-Authenticate") == "Keystone uri=http://" + identityEndpoint.hostname + ":" + properties.identityPort
        !reposeLogSearch.searchByString("INFO  highly-efficient-record-processor")
    }

    // Check all required attributes in the log
    private boolean checkAttribute(String jsonpart, List listattr) {
        boolean check = true
        for (attr in listattr) {
            if (!jsonpart.contains(attr)) {
                check = false
                break
            }
        }
        return check
    }
}
