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

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockTracerCollector
import org.rackspace.deproxy.Deproxy
import scaffold.category.Services
import spock.lang.Unroll

/**
 * Tests that sampling rate limiting type is set to 5 rps
 */
@Category(Services)
class OpenTracingServiceRateLimitedHttpTest extends ReposeValveTest {

    def static originEndpoint
    def static tracerEndpoint

    static MockTracerCollector fakeTracer

    static String TRACING_HEADER = "uber-trace-id"

    def static slurper = new groovy.json.JsonSlurper()


    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/opentracing/common", params)
        repose.configurationProvider.applyConfigs(
            "features/services/opentracing/enabledratelimiting/http",
            params + ['collectorTracingPort': properties.collectorTracingPort])

        originEndpoint = deproxy.addEndpoint(params.targetPort, 'origin service')

        fakeTracer = new MockTracerCollector(properties.collectorTracingPort)

        tracerEndpoint = deproxy.addEndpoint(properties.collectorTracingPort,
            'tracer http service', null, fakeTracer.handler)

        repose.start([waitOnJmxAfterStarting: false])
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll("Should report at most 5 spans per second with #method")
    def "when OpenTracing config is specified and enabled, trace information is passed in trace header"() {
        def traceCount = 0
        def traceList = []

        List<Thread> clientThreads = new ArrayList<Thread>()

        given:
        def thread = Thread.start {
            (0..<10).each {
                def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: method)
                if (messageChain.handlings.get(0).request.headers.contains(TRACING_HEADER)) {
                    traceCount++
                    traceList << URLDecoder.decode(
                        messageChain.handlings.get(0).request.headers.getFirstValue(TRACING_HEADER),
                        "UTF-8")

                }
            }
        }

        when: "10 Requests are sent through repose"
        clientThreads.add(thread)

        then: "Request sent to origin should be rate limited"
        clientThreads*.join()
        traceCount == 10


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
    def "when OpenTracing is enabled, and invalid span is passed in, the new span replaces the old span and is passed through"() {

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            headers: ['uber-trace-id': trace_id ],
            method: method)

        then: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "request should have 1 tracer headers"
        messageChain.handlings.get(0).request.headers.getCountByName(TRACING_HEADER) == 1

        and: "Repose should return with a 200"
        messageChain.receivedResponse.code == "200"

        and: "trace id does not exist in the new trace"
        def newTraceId = messageChain.handlings.get(0).request.headers.getFirstValue(TRACING_HEADER)
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
    def "when OpenTracing is enabled, and valid span is passed in, new span is created as child"() {

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            headers: ['uber-trace-id': trace_id ],
            method: method)

        then: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "request should have 1 tracer headers"
        messageChain.handlings.get(0).request.headers.getCountByName(TRACING_HEADER) == 1

        and: "trace id exists in the new trace"
        def newTraceId = messageChain.handlings.get(0).request.headers.getFirstValue(TRACING_HEADER)
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
