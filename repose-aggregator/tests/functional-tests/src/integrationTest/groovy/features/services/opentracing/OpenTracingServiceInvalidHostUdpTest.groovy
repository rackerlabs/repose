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
import org.openrepose.framework.test.mocks.MockTracerCollector
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

class OpenTracingServiceInvalidHostUdpTest extends ReposeValveTest {

    def static originEndpoint
    def static tracerEndpoint

    static MockTracerCollector fakeTracer

    static String TRACING_HEADER = "uber-trace-id"

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/opentracing/common", params)
        repose.configurationProvider.applyConfigs(
            "features/services/opentracing/invalidhost/udp",
            params + ['agentTracingPort': properties.agentTracingPort])

        originEndpoint = deproxy.addEndpoint(params.targetPort, 'origin service')

        fakeTracer = new MockTracerCollector(properties.collectorTracingPort)

        tracerEndpoint = deproxy.addEndpoint(properties.collectorTracingPort,
            'tracer http service', null, fakeTracer.handler)

        repose.start(true, false, "repose", "node1")
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll("Should return 200 with #method")
    def "when OpenTracing config has invalid host, no trace information is passed in tracing header"() {

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: method)

        then: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "Repose should return with a 200"
        messageChain.receivedResponse.code == "200"

        and: "OpenTracingService has logged that span was sent to tracer"
        //TODO: right now this is a no-op.  We aren't told that the trace is lost.  Not good.  However, in future
        // releases, we should have a story to add support for dropwizard
        def traceId = URLDecoder.decode(messageChain.handlings.get(0).request.headers.getFirstValue(TRACING_HEADER), "UTF-8")
        def logLines = reposeLogSearch.searchByString("Span reported: $traceId")
        logLines.size() == 1


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
}
