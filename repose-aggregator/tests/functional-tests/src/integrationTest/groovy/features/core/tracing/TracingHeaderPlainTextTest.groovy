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

import groovy.json.JsonOutput
import org.apache.commons.codec.binary.Base64
import org.junit.experimental.categories.Category
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Core

import java.nio.charset.Charset

/**
 * Created by adrian on 8/18/16.
 */
@Category(Core)
class TracingHeaderPlainTextTest extends ReposeValveTest {

    def static originEndpoint

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/tracing", params)
        repose.configurationProvider.applyConfigs("features/core/tracing/plaintext", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')

        reposeLogSearch.cleanLog()
    }

    def 'Parse externally provided X-Trans-Id header and add the Request ID to the request'() {
        given:
        def tracingId = UUID.randomUUID().toString()
        def sessionId = UUID.randomUUID().toString()
        def jsonTracingHeader = JsonOutput.toJson([sessionId: sessionId, requestId: tracingId, user: 'a', domain: 'b'])
        def tracingHeader = Base64.encodeBase64String(jsonTracingHeader.getBytes(Charset.forName("UTF-8")))
        def headers = [(CommonHttpHeader.TRACE_GUID): tracingHeader]

        when: 'User passes a request through repose'
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/1212",
                method: 'GET',
                headers: headers)

        then: 'Make sure the request and response contain a new X-Trans-Id header'
        mc.receivedResponse.code == '200'
        mc.handlings[0].request.headers.getFirstValue(CommonHttpHeader.TRACE_GUID) == tracingHeader
        mc.handlings[0].request.headers.getFirstValue(CommonHttpHeader.REQUEST_ID) == tracingId
    }

    def 'Parse externally provided X-Trans-Id header and overwrite the Request ID on the request'() {
        given:
        def tracingId = UUID.randomUUID().toString()
        def sessionId = UUID.randomUUID().toString()
        def jsonTracingHeader = JsonOutput.toJson([sessionId: sessionId, requestId: tracingId, user: 'a', domain: 'b'])
        def tracingHeader = Base64.encodeBase64String(jsonTracingHeader.getBytes(Charset.forName("UTF-8")))
        def headers = [(CommonHttpHeader.TRACE_GUID): tracingHeader,
                       (CommonHttpHeader.REQUEST_ID): "bob"]

        when: 'User passes a request through repose'
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/1212",
                method: 'GET',
                headers: headers)

        then: 'Make sure the request and response contain a new X-Trans-Id header'
        mc.receivedResponse.code == '200'
        mc.handlings[0].request.headers.getFirstValue(CommonHttpHeader.TRACE_GUID) == tracingHeader
        mc.handlings[0].request.headers.getFirstValue(CommonHttpHeader.REQUEST_ID) == tracingId
    }
}
