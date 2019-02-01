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
package features.core.powerfilter

import org.junit.experimental.categories.Category
import org.openrepose.commons.utils.logging.TracingHeaderHelper
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Core

@Category(Core)
class TracingHeaderTest extends ReposeValveTest {

    def setupSpec() {
        reposeLogSearch.cleanLog()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/tracing", params)
        repose.start()
    }

    def "should pass a tracing header through the filter chain if one was provided"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: ["x-trans-id": "test-guid"])

        then:
        mc.getHandlings().get(0).getRequest().getHeaders().getFirstValue("x-trans-id").equals("test-guid")
    }

    def "should pass a new tracing header through the filter chain if one was not provided"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: [:])

        then:
        TracingHeaderHelper.getTraceGuid(mc.getHandlings().get(0).getRequest().getHeaders().getFirstValue("x-trans-id")).matches(".+-.+-.+-.+-.+")
    }

    def "should return a tracing header if one was provided"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: ["x-trans-id": "test-guid"])

        then:
        mc.getReceivedResponse().getHeaders().getFirstValue("x-trans-id").equals("test-guid")
    }

    def "should return a tracing header if one was not provided"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: [:])
        def requestid = TracingHeaderHelper.getTraceGuid(mc.getReceivedResponse().getHeaders().getFirstValue("x-trans-id"))
        println(requestid)

        then:
        requestid.matches(".+-.+-.+-.+-.+")
        //**tracing header log REP-1828
        reposeLogSearch.searchByString("GUID:$requestid -.*").size() > 0
        reposeLogSearch.searchByString("Tracing header: ").size() == 0
    }
}
