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
package features.services.opentracing

import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.openrepose.framework.test.mocks.MockTracer
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

class OpenTracingServiceKeystoneTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    static MockTracer fakeTracer
    static MockIdentityV2Service fakeIdentityV2Service

    def static slurper = new groovy.json.JsonSlurper()


    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/opentracing/common", params)
        repose.configurationProvider.applyConfigs("features/services/opentracing/withkeystone", params)

        originEndpoint = deproxy.addEndpoint(params.targetPort, 'origin service')

        fakeTracer = new MockTracer(params.tracingPort, true)
        fakeIdentityV2Service = new MockIdentityV2Service(params.identityPort, params.targetPort)
        identityEndpoint = deproxy.addEndpoint(params.identityPort,
            'identity service', null, fakeIdentityV2Service.handler)

        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll("Should return 200 with #method")
    def "when OpenTracing config is specified and enabled with keystone-v2, trace information is passed in x-trans-id header"() {
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = "mytenant"
            client_tenantname = "mytenantname"
            client_userid = "12345"
        }

        def spanList = []

        when: "User passes a request through repose with valid token"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/servers/test", method: 'GET',
            headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "keystone request contains request id, uber-trace-id and origin keys"
        def keystoneCalls = messageChain.orphanedHandlings.size()

        (0..<keystoneCalls).each {
            def keystoneTransIdByteArray = messageChain.handlings.get(0).request.headers.getFirstValue("x-trans-id").decodeBase64()
            def keystoneTransIdObject = slurper.parse(keystoneTransIdByteArray)
            assert keystoneTransIdObject.keySet().size() == 3
            assert keystoneTransIdObject.keySet().contains("requestId")
            assert keystoneTransIdObject.keySet().contains("origin")
            assert keystoneTransIdObject.keySet().contains("uber-trace-id")
            assert keystoneTransIdObject.requestId != null
            assert keystoneTransIdObject.origin == null
            spanList << keystoneTransIdObject["uber-trace-id"]
        }


        and: "OpenTracingService has logged that keystone span was sent to tracer"
        spanList.each {
            def logLines = reposeLogSearch.searchByString("Span reported: $it")
            assert logLines.size() == 1
        }


        and: "request should have x-trans-id header"
        messageChain.handlings.get(0).request.headers.contains("x-trans-id")

        and: "request contains request id, uber-trace-id and origin keys"
        def originTransIdByteArray = messageChain.handlings.get(0).request.headers.getFirstValue("x-trans-id").decodeBase64()
        def originTransIdObject = slurper.parse(originTransIdByteArray)
        originTransIdObject.keySet().size() == 3
        originTransIdObject.keySet().contains("requestId")
        originTransIdObject.keySet().contains("origin")
        originTransIdObject.keySet().contains("uber-trace-id")
        originTransIdObject.requestId != null
        originTransIdObject.origin == null
        def originTraceId = originTransIdObject["uber-trace-id"]

        and: "OpenTracingService has logged that origin span was sent to tracer"
        def originLogLines = reposeLogSearch.searchByString("Span reported: $originTraceId")
        originLogLines.size() == 1

        and: "Repose should return with a 200"
        messageChain.receivedResponse.code == "200"


        where:
        method << ["GET", "PUT", "POST", "PATCH", "DELETE", "TRACE", "HEAD"]
    }
}
