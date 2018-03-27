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
import org.openrepose.framework.test.mocks.MockTracerAgent
import org.openrepose.framework.test.mocks.MockTracerCollector
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

class OpenTracingServiceHappyPathHttpTest extends ReposeValveTest {

    def static originEndpoint
    def static tracerEndpoint

    static MockTracerCollector fakeTracer

    def static slurper = new groovy.json.JsonSlurper()

    static String TRACING_HEADER = "uber-trace-id"


    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/opentracing/common", params)
        repose.configurationProvider.applyConfigs(
            "features/services/opentracing/enabled/http",
            params + ['collectorTracingPort': properties.collectorTracingPort])

        originEndpoint = deproxy.addEndpoint(params.targetPort, 'origin service')

        fakeTracer = new MockTracerCollector(properties.collectorTracingPort)

        tracerEndpoint = deproxy.addEndpoint(properties.collectorTracingPort,
            'tracer http service', null, fakeTracer.handler)

        repose.start([waitOnJmxAfterStarting: false])
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll("Should return 200 with #method")
    def "when OpenTracing is enabled, new span is created"() {

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: method, headers: [])

        then: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "OpenTracingService has logged that span was sent to tracer"
        def traceId = URLDecoder.decode(messageChain.handlings.get(0).request.headers.getFirstValue(TRACING_HEADER), "UTF-8")
        def logLines = reposeLogSearch.searchByString("Span reported: $traceId")
        logLines.size() == 1

        and: "Repose should return with a 200"
        messageChain.receivedResponse.code == "200"


        where:
        method   | _
        "GET"    | _
        "PUT"    | _
        "POST"   | _
        "PATCH"  | _
        "DELETE" | _
        "TRACE"  | _
        "HEAD"   | _
    }

    @Unroll("Should return 200 with #method with a trace id in request #trace_id")
    def "when OpenTracing is enabled, and valid span is passed in, new span is created and old span is passed through"() {

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            headers: [(TRACING_HEADER): trace_id ],
            method: method)

        then: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "request should have 2 tracer headers"
        if (trace_id == null)
            assert messageChain.handlings.get(0).request.headers.getCountByName(TRACING_HEADER) == 1
        else
            assert messageChain.handlings.get(0).request.headers.getCountByName(TRACING_HEADER) == 2

        and: "request should have tracer header pass through as well as a new header added"
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

        and: "trace id does not exist in the new trace"
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


    @Unroll("Should return 200 with #method with a trace id in request #trace_id")
    def "when OpenTracing is enabled, and invalid span is passed in, new span is created as child"() {

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            headers: [(TRACING_HEADER): trace_id ],
            method: method)

        then: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "request should have 2 tracer headers"
        messageChain.handlings.get(0).request.headers.getCountByName(TRACING_HEADER) == 2


        and: "request should have tracer header pass through as well as a new header added"
        def newTraceId
        def validateCount = 0
        def validateUniqueList = []
        messageChain.handlings.get(0).request.headers.each {
            if (it.name == TRACING_HEADER) {
                if (!validateUniqueList.contains(it.value)) validateUniqueList << it.value
                if (it.value == trace_id) validateCount++
                else newTraceId = it.value
            }
        }

        validateUniqueList.size() == messageChain.handlings.get(0).request.headers.getCountByName(TRACING_HEADER)
        validateCount == 1

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
