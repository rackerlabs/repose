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
import org.openrepose.framework.test.mocks.MockTracerAgent
import org.openrepose.framework.test.mocks.MockTracerCollector
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

class OpenTracingServiceKeystoneHttpTest extends ReposeValveTest {

    def static originEndpoint
    def static tracerEndpoint
    def static identityEndpoint

    static MockTracerCollector fakeTracer

    static MockIdentityV2Service fakeIdentityV2Service

    static String TRACING_HEADER = "uber-trace-id"

    def static slurper = new groovy.json.JsonSlurper()


    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/opentracing/common", params)
        repose.configurationProvider.applyConfigs("features/services/opentracing/withkeystone", params)
        repose.configurationProvider.applyConfigs(
            "features/services/opentracing/withkeystone/http",
            params + ['collectorTracingPort': properties.collectorTracingPort])

        originEndpoint = deproxy.addEndpoint(params.targetPort, 'origin service')

        fakeIdentityV2Service = new MockIdentityV2Service(params.identityPort, params.targetPort)
        identityEndpoint = deproxy.addEndpoint(params.identityPort,
            'identity service', null, fakeIdentityV2Service.handler)

        fakeTracer = new MockTracerCollector(properties.collectorTracingPort)

        tracerEndpoint = deproxy.addEndpoint(properties.collectorTracingPort,
            'tracer http service', null, fakeTracer.handler)

        repose.start([waitOnJmxAfterStarting: false])
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll("Should return 200 with #method")
    def "when OpenTracing config is enabled with keystone-v2, trace information is passed in tracing header"() {
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

        and: "keystone request contains tracing header"
        messageChain.orphanedHandlings.each {
            // first, check to make sure it's not the tracer request (since that's done asynchronously and may count in orphaned handlings)
            if (it.request.path != "/?format=jaeger.thrift") {
                assert it.request.headers.contains(TRACING_HEADER)
                def traceId = URLDecoder.decode(it.request.headers.getFirstValue(TRACING_HEADER), "UTF-8")
                spanList << traceId
            }
        }


        and: "OpenTracingService has logged that keystone span was sent to tracer"
        spanList.each {
            def logLines = reposeLogSearch.searchByString("Span reported: $it")
            assert logLines.size() == 1
        }

        and: "request should have tracing header"
        messageChain.handlings.get(0).request.headers.contains(TRACING_HEADER)

        and: "OpenTracingService has logged that span was sent to tracer"
        def traceId = URLDecoder.decode(messageChain.handlings.get(0).request.headers.getFirstValue(TRACING_HEADER), "UTF-8")
        def logLines = reposeLogSearch.searchByString("Span reported: $traceId")
        logLines.size() == 1

        and: "Repose should return with a 200"
        messageChain.receivedResponse.code == "200"

        where:
        method << ["GET", "PUT", "POST", "PATCH", "DELETE", "TRACE", "HEAD"]
    }

    @Unroll("Should return 200 with #method and #trace_id")
    def "when OpenTracing config is enabled with keystone-v2, with invald parent span, trace information is passed in tracing header"() {
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
            headers: [
                'content-type': 'application/json',
                'X-Auth-Token': fakeIdentityV2Service.client_token,
                (TRACING_HEADER): trace_id
            ])

        then: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "keystone request contains tracing header"
        messageChain.orphanedHandlings.each {
            // first, check to make sure it's not the tracer request (since that's done asynchronously and may count in orphaned handlings)
            if (it.request.path != "/?format=jaeger.thrift") {
                assert it.request.headers.getCountByName(TRACING_HEADER) == 1
                spanList << it.request.headers.getFirstValue(TRACING_HEADER)
            }
        }


        and: "OpenTracingService has logged that keystone span was sent to tracer"
        spanList.each {
            def logLines = reposeLogSearch.searchByString(
                "Span reported: ${URLDecoder.decode(it, "UTF-8")}")
            assert logLines.size() == 1
        }

        and: "OpenTracingService passes a new trace id"
        spanList.each {
            assert it != trace_id

        }

        and: "request to origin should have 2 tracer headers"
        if (trace_id == null)
            assert messageChain.handlings.get(0).request.headers.getCountByName(TRACING_HEADER) == 1
        else
            assert messageChain.handlings.get(0).request.headers.getCountByName(TRACING_HEADER) == 2

        and: "request to origin should have tracer header pass through as well as a new header added"
        def newTraceId
        if (trace_id == null) {
            newTraceId = messageChain.handlings.get(0).request.headers.getFirstValue(TRACING_HEADER)
        } else {
            def validateCount = 0
            messageChain.handlings.get(0).request.headers.each {
                if (it.name == TRACING_HEADER) {
                    if (it.value == trace_id) validateCount++
                    else newTraceId = it.value
                }
            }
            assert validateCount == 1
        }


        and: "Repose should return with a 200"
        messageChain.receivedResponse.code == "200"

        and: "trace id does not exist in the new trace in request to origin"
        assert newTraceId != trace_id

        where:
        method   | trace_id
        "HEAD"   | 'fake'
        "GET"    | 'fake'
        "PUT"    | 'fake'
        "POST"   | 'fake'
        "PATCH"  | 'fake'
        "DELETE" | 'fake'
        "TRACE"  | 'fake'
        "HEAD"   | null
        "GET"    | null
        "PUT"    | null
        "POST"   | null
        "PATCH"  | null
        "DELETE" | null
        "TRACE"  | null
        "HEAD"   | null
    }


    @Unroll("Should return 200 with #method and #trace_id")
    def "when OpenTracing config is enabled with keystone-v2, with parent span, trace information is passed in tracing header"() {
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
            headers: [
                'content-type': 'application/json',
                'X-Auth-Token': fakeIdentityV2Service.client_token,
                (TRACING_HEADER): trace_id
            ])

        then: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "keystone request contains tracing header"
        messageChain.orphanedHandlings.each {
            // first, check to make sure it's not the tracer request (since that's done asynchronously and may count in orphaned handlings)
            if (it.request.path != "/?format=jaeger.thrift") {
                assert it.request.headers.getCountByName(TRACING_HEADER) == 1
                spanList << it.request.headers.getFirstValue(TRACING_HEADER)
            }
        }


        and: "OpenTracingService has logged that keystone span was sent to tracer"
        spanList.each {
            def logLines = reposeLogSearch.searchByString(
                "Span reported: ${URLDecoder.decode(it, "UTF-8")}")
            assert logLines.size() == 1
        }

        and: "OpenTracingService passes a new trace id"
        spanList.each {
            assert it != trace_id

        }

        and: "request to origin should have 2 tracer headers"
        if (trace_id == null)
            assert messageChain.handlings.get(0).request.headers.getCountByName(TRACING_HEADER) == 1
        else
            assert messageChain.handlings.get(0).request.headers.getCountByName(TRACING_HEADER) == 2

        and: "request to origin should have tracer header pass through as well as a new header added"
        def newTraceId
        if (trace_id == null) {
            newTraceId = messageChain.handlings.get(0).request.headers.getFirstValue(TRACING_HEADER)
        } else {
            def validateCount = 0
            messageChain.handlings.get(0).request.headers.each {
                if (it.name == TRACING_HEADER) {
                    if (it.value == trace_id) validateCount++
                    else newTraceId = it.value
                }
            }
            assert validateCount == 1
        }


        and: "Repose should return with a 200"
        messageChain.receivedResponse.code == "200"

        and: "trace id exists in the new trace"
        if (trace_id.contains(':')) {
            def traceId = trace_id.split(":").first()
            assert newTraceId.split("%3A").first() == traceId
        } else {
            def traceId = trace_id.split("%3A").first()
            assert newTraceId.split("%3A").first() == traceId
        }

        and: "Repose should return with a 200"
        messageChain.receivedResponse.code == "200"


        where:
        method   | trace_id
        "GET"    | '5f074a7cedaff647%3A6cfe3defc2e78be5%3A5f074a7cedaff647%3A1'
        "PUT"    | '5f074a7cedaff647%3A6cfe3defc2e78be5%3A5f074a7cedaff647%3A1'
        "POST"   | '5f074a7cedaff647%3A6cfe3defc2e78be5%3A5f074a7cedaff647%3A1'
        "PATCH"  | '5f074a7cedaff647%3A6cfe3defc2e78be5%3A5f074a7cedaff647%3A1'
        "DELETE" | '5f074a7cedaff647%3A6cfe3defc2e78be5%3A5f074a7cedaff647%3A1'
        "TRACE"  | '5f074a7cedaff647%3A6cfe3defc2e78be5%3A5f074a7cedaff647%3A1'
        "HEAD"   | '5f074a7cedaff647%3A6cfe3defc2e78be5%3A5f074a7cedaff647%3A1'
        "GET"    | '5f074a7cedaff647:6cfe3defc2e78be5:5f074a7cedaff647:1'
        "PUT"    | '5f074a7cedaff647:6cfe3defc2e78be5:5f074a7cedaff647:1'
        "POST"   | '5f074a7cedaff647:6cfe3defc2e78be5:5f074a7cedaff647:1'
        "PATCH"  | '5f074a7cedaff647:6cfe3defc2e78be5:5f074a7cedaff647:1'
        "DELETE" | '5f074a7cedaff647:6cfe3defc2e78be5:5f074a7cedaff647:1'
        "TRACE"  | '5f074a7cedaff647:6cfe3defc2e78be5:5f074a7cedaff647:1'

    }
}
