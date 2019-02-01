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

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.openrepose.framework.test.mocks.MockTracerCollector
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Services
import spock.lang.Unroll

@Category(Services)
class UriRedactionServiceKeystoneTest extends ReposeValveTest {

    static MockTracerCollector fakeTracer
    static MockIdentityV2Service fakeIdentityV2Service
    static List<String> spanList

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
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = "mytenant"
            client_tenantname = "mytenantname"
            client_userid = "12345"
        }
        spanList = []
    }

    @Unroll
    def "when a call is made that hits #name the uri should be redacted"() {
        when: "User passes a request through repose with valid token"
        MessageChain messageChain = deproxy.makeRequest(url: "$reposeEndpoint$requestedPath",
            headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "keystone request contains tracing header"
        // make sure it's not the tracer request (since that's done asynchronously and may count in orphaned handlings)
        messageChain.orphanedHandlings.findAll({ it.request.path != "/?format=jaeger.thrift" }).each {
            assert it.request.headers.contains(TRACING_HEADER)
            def traceId = URLDecoder.decode(it.request.headers.getFirstValue(TRACING_HEADER), "UTF-8")
            spanList << traceId
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
        assertUntilTrue({
            assert fakeTracer.batches.collect({ it.spans }).flatten().collect({ it.operationName }).any({
                it.contains(expectedResult)
            })
        })

        where:
        name                                                               | requestedPath         || expectedResult
        "keystone"                                                         | "/servers/test"       || '/v2.0/tokens/XXXXX'
        "against a uri with multiple capture groups"                       | "/foo/bar/baz"        || '/XXXXX/bar/XXXXX'
        "against multiple regexes"                                         | "/some/specific/path" || '/XXXXX/specific/XXXXX'
        "against a regex with a segment repeated only the intended one in" | "/path/specific/path" || '/path/specific/XXXXX'
    }

    void assertUntilTrue(Closure assertion, long timeout = 10000, long waitTime = 500) {
        def finishTime = System.currentTimeMillis() + timeout
        while (true) {
            try {
                assertion()
                break
            } catch (Throwable t) {
                if (System.currentTimeMillis() > finishTime) {
                    assertion()
                } else {
                    sleep(waitTime)
                }
            }
        }
    }
}
