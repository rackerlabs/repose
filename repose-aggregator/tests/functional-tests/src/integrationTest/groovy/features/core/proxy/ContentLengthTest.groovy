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
package features.core.proxy

import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

class ContentLengthTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy", params)
        repose.configurationProvider.applyConfigs("features/core/proxy/chunkedfalse", params)
        repose.start()
    }

    @Unroll("When set to #method chunked encoding to false and sending #reqBody.")
    def "When set to send chunked encoding to false. Repose should not send requests chunked"() {
        when:
        def messageChain = deproxy.makeRequest(
                url: reposeEndpoint,
                method: method,
                headers: [["Content-Type": "plain/text"]],
                requestBody: reqBody
        )
        def clientRequestHeaders = messageChain.sentRequest.headers
        def originRequestHeaders = messageChain.getHandlings()[0].request.headers

        then:
        clientRequestHeaders.findAll("Transfer-Encoding").size() == 0
        originRequestHeaders.findAll("Transfer-Encoding").size() == 0
        originRequestHeaders.findAll("Content-Type").size() == 1
        originRequestHeaders.findAll("Content-Length").size() == (!method.equalsIgnoreCase("TRACE") ? 1 : 0)

        if (originRequestHeaders.findAll("Content-Length").size() > 0)
            assert (originRequestHeaders.getFirstValue("Content-Length").equalsIgnoreCase((reqBody == null) ? "0" : reqBody.length().toString()))

        where:
        [method, reqBody] << [["POST", "PUT", "TRACE"], ["blah", null]].combinations()
    }

    @Unroll("should not send chunked request for incoming chunked #method request with request body: #reqBody")
    def "when chunked encoding is set to false and the incoming request is chunked, Repose should not send chunked requests"() {
        when:
        def messageChain = deproxy.makeRequest(
                url: reposeEndpoint,
                method: method,
                headers: [["Transfer-Encoding": "chunked"], ["Content-Type": "plain/text"]],
                requestBody: reqBody,
                chunked: true
        )
        def clientRequestHeaders = messageChain.sentRequest.headers
        def originRequestHeaders = messageChain.getHandlings()[0].request.headers

        then:
        clientRequestHeaders.findAll("Transfer-Encoding").size() == 1
        originRequestHeaders.findAll("Transfer-Encoding").size() == 0
        originRequestHeaders.findAll("Content-Type").size() == 1
        originRequestHeaders.findAll("Content-Length").size() == (!method.equalsIgnoreCase("TRACE") ? 1 : 0)

        if (originRequestHeaders.findAll("Content-Length").size())
            originRequestHeaders.getFirstValue("Content-Length").toInteger() == ((reqBody == null) ? 0 : reqBody.length())

        where:
        [method, reqBody] << [["POST", "PUT", "TRACE"], ["blah", null]].combinations()
    }
}
