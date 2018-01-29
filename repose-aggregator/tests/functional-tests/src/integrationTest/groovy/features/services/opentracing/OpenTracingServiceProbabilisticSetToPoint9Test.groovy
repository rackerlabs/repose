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
import org.openrepose.framework.test.mocks.MockTracer
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

/**
 * Tests that sampling probably type is set to 0.9 (90% of requests get reported)
 */
class OpenTracingServiceProbabilisticSetToPoint9Test extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    static MockTracer fakeTracer

    static String TRACING_HEADER = "uber-trace-id"

    def static slurper = new groovy.json.JsonSlurper()


    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/opentracing/common", params)
        repose.configurationProvider.applyConfigs("features/services/opentracing/enabledprobabilisticpoint9", params)

        originEndpoint = deproxy.addEndpoint(params.targetPort, 'origin service')

        fakeTracer = new MockTracer(params.tracingPort, true)

        repose.start([waitOnJmxAfterStarting: false])
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll("Should contain span 90% of the time with #method")
    def "when OpenTracing config is specified and enabled, trace information is passed in tracing header"() {
        def traceCount = 0

        when: "10 Requests are sent through repose"
        (0..<10).each {
            def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: method)
            if (messageChain.handlings.get(0).request.headers.contains(TRACING_HEADER)) {
                traceCount++
            }
        }

        then: "Request sent to origin will always contain span header (sampling done on the collector side)"
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
}
