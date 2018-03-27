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
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

class OpenTracingServiceNoConfigTest extends ReposeValveTest {

    def static originEndpoint

    def static slurper = new groovy.json.JsonSlurper()

    static String TRACING_HEADER = "uber-trace-id"


    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/opentracing/common", params)

        originEndpoint = deproxy.addEndpoint(params.targetPort, 'origin service')

        repose.start([waitOnJmxAfterStarting: false])
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll("Should return 200 with #method")
    def "when no opentracing config is specified, no trace information is passed in tracing header"() {

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: method)

        then: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "request should not have tracing header"
        !messageChain.handlings.get(0).request.headers.contains(TRACING_HEADER)

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


    @Unroll("Should return 200 with #method with a trace id #trace_id")
    def "when OpenTracing config is not specified, and trace id passed in, new span is not created, and trace id is passed through"() {

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            headers: [(TRACING_HEADER): trace_id ],
            method: method)

        then: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "request should have tracer header equal to passed in header (we did not create another one in repose)"

        if (trace_id != null) {
            assert messageChain.handlings.get(0).request.headers.contains(TRACING_HEADER)
            assert messageChain.handlings.get(0).request.headers.getFirstValue(TRACING_HEADER) == trace_id
        } else {
            assert !messageChain.handlings.get(0).request.headers.contains(TRACING_HEADER)
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
}

