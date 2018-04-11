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
package features.services.uriredaction

import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.openrepose.framework.test.mocks.MockTracerCollector
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class UriRedactionServiceKeystoneTest extends ReposeValveTest {

    static MockTracerCollector fakeTracer
    static MockIdentityV2Service fakeIdentityV2Service

    static String TRACING_HEADER = "uber-trace-id"

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs(
            "features/services/uriredaction",
            params + ['collectorTracingPort': properties.collectorTracingPort])

        deproxy.addEndpoint(params.targetPort, 'origin service')

        fakeIdentityV2Service = new MockIdentityV2Service(params.identityPort, params.targetPort)
        deproxy.addEndpoint(params.identityPort,
            'identity service', null, fakeIdentityV2Service.handler)

        fakeTracer = new MockTracerCollector(properties.collectorTracingPort)

        deproxy.addEndpoint(properties.collectorTracingPort,
            'tracer http service', null, fakeTracer.handler)

        repose.start(waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def setup() {
        fakeTracer.batches.clear()
    }

    def "when calls are made to keystone the uri should be redacted"() {
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

        and: "OpenTracingService has logged that span was sent to tracer"
        def traceId = URLDecoder.decode(messageChain.handlings.get(0).request.headers.getFirstValue(TRACING_HEADER), "UTF-8")
        def logLines = reposeLogSearch.searchByString("Span reported: $traceId")
        logLines.size() == 1

        and: "The sent trace doesn't have the un-redacted token in it"
        fakeTracer.batches.any({ it.spans.collect({ it.getOperationName() }).contains("/v2.0/tokens/XXXXX") })
    }

    def "when a call is made that hits against a uri with multiple capture groups the uri should redacted"() {
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = "mytenant"
            client_tenantname = "mytenantname"
            client_userid = "12345"
        }

        def spanList = []

        when: "User passes a request through repose with valid token"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/foo/bar/baz", method: 'GET',
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

        and: "OpenTracingService has logged that span was sent to tracer"
        def traceId = URLDecoder.decode(messageChain.handlings.get(0).request.headers.getFirstValue(TRACING_HEADER), "UTF-8")
        def logLines = reposeLogSearch.searchByString("Span reported: $traceId")
        logLines.size() == 1

        and: "The sent trace doesn't have the un-redacted token in it"
        fakeTracer.batches.any({ it.spans.collect({ it.getOperationName() }).contains("/v2.0/tokens/XXXXX") })
    }

    def "when a call is made that hits against multiple regexes the uri should redacted"() {
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = "mytenant"
            client_tenantname = "mytenantname"
            client_userid = "12345"
        }

        def spanList = []

        when: "User passes a request through repose with valid token"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/some/specific/path", method: 'GET',
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

        and: "OpenTracingService has logged that span was sent to tracer"
        def traceId = URLDecoder.decode(messageChain.handlings.get(0).request.headers.getFirstValue(TRACING_HEADER), "UTF-8")
        def logLines = reposeLogSearch.searchByString("Span reported: $traceId")
        logLines.size() == 1

        and: "The sent trace doesn't have the un-redacted token in it"
        fakeTracer.batches.any({ it.spans.collect({ it.getOperationName() }).contains("/v2.0/tokens/XXXXX") })
    }
}
