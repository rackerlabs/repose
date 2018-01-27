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
import org.rackspace.deproxy.Handlers
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Tests that sampling const type is set to 0 (nothing gets traced)
 */
class OpenTracingServiceRateLimitedTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    static MockTracer fakeTracer

    def static slurper = new groovy.json.JsonSlurper()


    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/opentracing/common", params)
        repose.configurationProvider.applyConfigs("features/services/opentracing/enabledratelimiting", params)

        originEndpoint = deproxy.addEndpoint(params.targetPort, 'origin service')

        fakeTracer = new MockTracer(params.tracingPort, true)

        repose.start([waitOnJmxAfterStarting: false])
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll("Should contain span 90% of the time with #method")
    def "when OpenTracing config is specified and enabled, trace information is passed in x-trans-id header"() {
        def traceCount = 0
        def traceList = []

        List<Thread> clientThreads = new ArrayList<Thread>()

        given:
        def thread = Thread.start {
            (0..<10).each {
                def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: method)
                def transIdByteArray = messageChain.handlings.get(0).request.headers.getFirstValue("x-trans-id").decodeBase64()
                def transIdObject = slurper.parse(transIdByteArray)
                if (transIdObject.keySet().contains("uber-trace-id")) {
                    traceCount++
                    traceList << transIdObject["uber-trace-id"]
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
}
