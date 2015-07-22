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
package features.core.tracing

import framework.ReposeValveTest
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class RewriteTracingHeaderTest extends ReposeValveTest {

    def setupSpec() {
        reposeLogSearch.cleanLog()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/tracing", params)
        repose.configurationProvider.applyConfigs("features/core/tracing/rewritetransid", params)

        repose.start()
    }

    def "should not pass the externally provided tracing header through the filter chain"() {
        when:
        MessageChain mc = deproxy.makeRequest(
                url: reposeEndpoint, headers: [(CommonHttpHeader.TRACE_GUID.toString()): 'test-guid-for-rewrite'])
        def requestId = mc.getHandlings().get(0).getRequest().getHeaders().getFirstValue(
                CommonHttpHeader.TRACE_GUID.toString())

        then:
        !requestId.equals('test-guid-for-rewrite')
        requestId.matches('.+-.+-.+-.+-.+')
    }

    def "should pass a new tracing header through the filter chain if one was not provided"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: [:])

        then:
        mc.getHandlings().get(0).getRequest().getHeaders().getFirstValue(CommonHttpHeader.TRACE_GUID.toString())
                .matches('.+-.+-.+-.+-.+')
    }

    def "should not return the externally provided tracing header if one was provided"() {
        when:
        MessageChain mc = deproxy.makeRequest(
                url: reposeEndpoint, headers: [(CommonHttpHeader.TRACE_GUID.toString()): 'test-guid-for-rewrite'])
        def requestId = mc.getReceivedResponse().getHeaders().getFirstValue(CommonHttpHeader.TRACE_GUID.toString())

        then:
        !requestId.equals('test-guid-for-rewrite')
        requestId.matches('.+-.+-.+-.+-.+')
    }

    def "should return a tracing header if one was not provided"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: [:])
        def requestId = mc.getReceivedResponse().getHeaders().getFirstValue(CommonHttpHeader.TRACE_GUID.toString())

        then:
        requestId.matches('.+-.+-.+-.+-.+')
    }
}
