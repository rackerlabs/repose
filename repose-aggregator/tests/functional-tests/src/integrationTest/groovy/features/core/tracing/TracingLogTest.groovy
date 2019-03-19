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
package features.core.tracing

import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.openrepose.commons.utils.logging.TracingHeaderHelper
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Core

/**
 * Specific tests for admin token
 */
@Category(Core)
class TracingLogTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/tracing", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)

        reposeLogSearch.cleanLog()
    }

    def setup() {
        waitForHttpClientRequestCacheToClear()
        fakeIdentityService.resetHandlers()
    }

    def "Making a call through the HTTP client should include the GUID generated in the request"() {

        given:
        fakeIdentityService.with {
            client_tenantid = 1111
            client_userid = 1111
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }


        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/1111/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )
        // get tracing header from request
        def requestid = mc.handlings[0].request.headers.getFirstValue("x-trans-id")

        then: "Make sure there are appropriate log messages with matching GUIDs"
        mc.receivedResponse.code == "200"

        //Find the GUID out of :  Trans-Id:e6a7f92b-1d22-4f97-8367-7787ccb5f100 - 2015-05-20 12:07:14,045 68669 [qtp172333204-48] DEBUG org.openrepose.filters.clientauth.common.AuthenticationHandler - Uri is /servers/1111/
        List<String> lines1 = reposeLogSearch.searchByString("Trans-Id:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12} - .* - checking /servers/1111/ against \\^\\\$")
        List<String> lines2 = reposeLogSearch.searchByString("Trans-Id:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12} - .* - checking /servers/1111/ against /buildinfo")
        List<String> lines3 = reposeLogSearch.searchByString("Trans-Id:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12} - .* - checking /servers/1111/ against /get")

        lines1.size() == 1
        lines2.size() == 1
        lines3.size() == 1

        //Ensure that GUID is used in a log message for the HTTP request
        String GUID = (lines1.first() =~ "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})")[0][1]
        def actorLines = reposeLogSearch.searchByString("Trans-Id:$GUID -.*org.apache.http.wire.*Host.*${fakeIdentityService.port}")
        actorLines.size() == 3
    }

    def "Making a request through the HTTP client tracing header should same as log"() {

        given:
        fakeIdentityService.with {
            client_tenantid = 1212
            client_userid = 1212
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }


        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/1212/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )
        // get tracing header from request
        def requestid = TracingHeaderHelper.getTraceGuid(mc.handlings[0].request.headers.getFirstValue("x-trans-id"))
        println requestid

        then: "Make sure there are appropriate log messages with matching GUIDs"
        mc.receivedResponse.code == "200"

        // should be able to find the same tracing header from log
        reposeLogSearch.searchByString("Trans-Id:$requestid -.*org.apache.http.wire").size() > 0
    }
}
