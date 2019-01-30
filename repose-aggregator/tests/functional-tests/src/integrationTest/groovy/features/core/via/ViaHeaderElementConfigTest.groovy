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

package features.core.via

import org.apache.http.HttpResponse
import org.apache.http.HttpVersion
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.HttpClients
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.server.CustomizableSocketServerConnector
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Endpoint
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Core
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Unroll

import static CustomizableSocketServerConnector.HTTP_1_0
import static CustomizableSocketServerConnector.HTTP_1_1
import static javax.servlet.http.HttpServletResponse.SC_OK
import static org.rackspace.deproxy.Deproxy.REQUEST_ID_HEADER_NAME
import static org.springframework.http.HttpHeaders.VIA

/**
 * Tests the full range of inputs and outputs for one possible configuration of the via-header element.
 */
@Category(Core)
class ViaHeaderElementConfigTest extends ReposeValveTest {
    private static final String VIA_REQUEST_PREFIX = "Banichi"
    private static final String VIA_RESPONSE_PREFIX = "Jago"

    @Shared String reposePort
    @Shared String reposeVersion
    @Shared CustomizableSocketServerConnector socketServerConnector

    def setupSpec() {
        deproxy = new Deproxy()
        Endpoint originService = deproxy.addEndpoint(name: 'origin service', connectorFactory: { Endpoint endpoint ->
            new CustomizableSocketServerConnector(endpoint, properties.targetPort)
        })
        socketServerConnector = originService.serverConnector as CustomizableSocketServerConnector

        def params = properties.getDefaultTemplateParams() +
            [viaRequestPrefix: VIA_REQUEST_PREFIX, viaResponsePrefix: VIA_RESPONSE_PREFIX]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/via", params)
        repose.configurationProvider.applyConfigs("features/core/via/element", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)

        reposePort = properties.reposePort as String
        reposeVersion = properties.reposeVersion
    }

    def "for an HTTP/1.1 request from the client, the Via header in the request going to the origin service should contain the configured value"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint)

        then: "the request to the origin service should have a Via header with the given value"
        mc.handlings[0].request.headers.getFirstValue(VIA) == "1.1 $VIA_REQUEST_PREFIX (Repose/$reposeVersion)"
    }

    def "for an HTTP/1.1 response from the origin service, the Via header in the response going to the client should contain the configured value"() {
        given: "the origin service will return an HTTP/1.1 response"
        socketServerConnector.httpProtocol = HTTP_1_1

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint)

        then: "the response to the client should have a Via header with the given value"
        mc.receivedResponse.headers.getFirstValue(VIA) == "1.1 $VIA_RESPONSE_PREFIX (Repose/$reposeVersion)"
    }

    def "for an HTTP/1.0 request from the client, the Via header in the request going to the origin service should contain the configured value"() {
        given: "the client will make an HTTP/1.0 request"
        HttpClient client = HttpClients.createDefault()
        HttpUriRequest request = new HttpGet(reposeEndpoint)
        request.setProtocolVersion(HttpVersion.HTTP_1_0)

        and: "Deproxy will track the request to the origin service"
        MessageChain mc = new MessageChain()
        String requestId = UUID.randomUUID().toString()
        deproxy.addMessageChain(requestId, mc)
        request.addHeader(REQUEST_ID_HEADER_NAME, requestId)

        when:
        client.execute(request)

        then: "the request to the origin service should have a Via header with the given value"
        mc.handlings[0].request.headers.getFirstValue(VIA) == "1.0 $VIA_REQUEST_PREFIX (Repose/$reposeVersion)"

        cleanup:
        deproxy.removeMessageChain(requestId)
    }

    @Ignore("Repose does not support reading the HTTP Protocol of the origin service response but should (RFC 7230 - 5.7.1)")
    def "for an HTTP/1.0 response from the origin service, the Via header in the response going to the client should contain the configured value"() {
        given: "the origin service will return an HTTP/1.0 response"
        socketServerConnector.httpProtocol = HTTP_1_0

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint)

        then: "the response to the client should have a Via header with the given value"
        mc.receivedResponse.headers.getFirstValue(VIA) == "1.0 $VIA_RESPONSE_PREFIX (Repose/$reposeVersion)"
    }

    @Ignore("Repose purposely does not pass this test in order to maintain backwards compatibility but should (RFC 7230 - 5.7.1)")
    def "for an HTTP/1.0 request and HTTP/1.1 response, the Via header in the response going to the client should contain the configured value"() {
        given: "the client will make an HTTP/1.0 request"
        HttpClient client = HttpClients.createDefault()
        HttpUriRequest request = new HttpGet(reposeEndpoint)
        request.setProtocolVersion(HttpVersion.HTTP_1_0)

        and: "Deproxy will track the request to the origin service"
        MessageChain mc = new MessageChain()
        String requestId = UUID.randomUUID().toString()
        deproxy.addMessageChain(requestId, mc)
        request.addHeader(REQUEST_ID_HEADER_NAME, requestId)

        and: "the origin service will return an HTTP/1.1 response"
        socketServerConnector.httpProtocol = HTTP_1_1

        when:
        HttpResponse response = client.execute(request)

        then: "the response to the client should have a Via header with the given value"
        response.getFirstHeader(VIA).getValue() == "1.1 $VIA_RESPONSE_PREFIX (Repose/$reposeVersion)"

        cleanup:
        deproxy.removeMessageChain(requestId)
    }

    def "for an HTTP/1.0 request and response, the Via header in the response going to the client should contain the configured value"() {
        given: "the client will make an HTTP/1.0 request"
        HttpClient client = HttpClients.createDefault()
        HttpUriRequest request = new HttpGet(reposeEndpoint)
        request.setProtocolVersion(HttpVersion.HTTP_1_0)

        and: "Deproxy will track the request to the origin service"
        MessageChain mc = new MessageChain()
        String requestId = UUID.randomUUID().toString()
        deproxy.addMessageChain(requestId, mc)
        request.addHeader(REQUEST_ID_HEADER_NAME, requestId)

        and: "the origin service will return an HTTP/1.0 response"
        socketServerConnector.httpProtocol = HTTP_1_0

        when:
        HttpResponse response = client.execute(request)

        then: "the response to the client should have a Via header with the given value"
        response.getFirstHeader(VIA).getValue() == "1.0 $VIA_RESPONSE_PREFIX (Repose/$reposeVersion)"

        cleanup:
        deproxy.removeMessageChain(requestId)
    }

    @Unroll
    def "an existing Via header in the request from the client is not altered with client header: #clientVia"() {
        given: "the client request will contain a Via header"
        Map<String, String> headers = [(VIA): clientVia]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers)

        then: "the request to the origin service will have the original list of proxies and the new Repose value"
        String expectedNewValue = "1.1 $VIA_REQUEST_PREFIX (Repose/$reposeVersion)"
        List<String> actualViaHeaders = mc.handlings[0].request.headers.findAll(VIA)

        // test should not care if the Repose value was appended or added to the header, so accept either behavior
        (actualViaHeaders.size() == 1 && actualViaHeaders[0] == "$clientVia, $expectedNewValue") ||
            (actualViaHeaders.size() == 2 && actualViaHeaders[0] == clientVia && actualViaHeaders[1] == expectedNewValue)

        where:
        clientVia << [
            "1.1 ab.www.examplesite1.com, 1.1 ab.www.examplesite2.com",
            "1.1 potato.net(fancy) (FancyPotato)",
            "1.0 fred, 1.1 example.com (Apache/1.1)",
            "1.0 ricky, 1.1 ethel, 1.1 fred, 1.0 lucy",
            "1.0 api.example.xyz (Apache-Wolf/10.3)",
            "1.1 proxy63 (NetCache NetApp/1.2.345)",
            "1.1 1234567890a18392e8917f31example.frog.net (FrogsEverywhere)",
            "1.1 potato.cloud.dev.dfw1.derp.rackspace.com:123456 (Repose/8.5.0.0)",
            "1.0 Repose (Repose/7.3.5.0)",
            "1.1 Repose (Repose/7.1.3.2-SNAPSHOT)",
            "1.1 10.10.10.123:8080 (Repose/7.3.0.0)",
            "0.9 localhost:8080 (Repose/7.3.0.0)",
            "HTTP/1.1 LEGIT",
            "HTTP/2.0 pseudonym",
            $/HTTP/2.0 pseudonym (this, \(is\), a, comment)/$,
            "HTTP/2.0 pseudonym (yo (dawg (I heard (you (like) comments))))",
            "SIP/2.0/UDP 10.10.10.123:5060; branch=1234567890abc"]
    }

    @Unroll
    def "an existing Via header in the response from the origin service is not altered with server header: #originServiceVia"() {
        given: "the origin service will return the given value in the Via header"
        Closure<Response> originService = { new Response(SC_OK, null, [(VIA): originServiceVia]) }

        and: "the origin service will return an HTTP/1.1 response"
        socketServerConnector.httpProtocol = HTTP_1_1

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, defaultHandler: originService)

        then: "the response to the client will have the original list of proxies and the new Repose value"
        String expectedNewValue = "1.1 $VIA_RESPONSE_PREFIX (Repose/$reposeVersion)"
        List<String> actualViaHeaders = mc.receivedResponse.headers.findAll(VIA)

        // test should not care if the Repose value was appended or added to the header, so accept either behavior
        (actualViaHeaders.size() == 1 && actualViaHeaders[0] == "$originServiceVia, $expectedNewValue") ||
            (actualViaHeaders.size() == 2 && actualViaHeaders[0] == originServiceVia && actualViaHeaders[1] == expectedNewValue)

        where:
        originServiceVia << [
            "1.1 ab.www.examplesite1.com, 1.1 ab.www.examplesite2.com",
            "1.1 potato.net(fancy) (FancyPotato)",
            "1.0 fred, 1.1 example.com (Apache/1.1)",
            "1.0 ricky, 1.1 ethel, 1.1 fred, 1.0 lucy",
            "1.0 api.example.xyz (Apache-Wolf/10.3)",
            "1.1 proxy63 (NetCache NetApp/1.2.345)",
            "1.1 1234567890a18392e8917f31example.frog.net (FrogsEverywhere)",
            "1.1 potato.cloud.dev.dfw1.derp.rackspace.com:123456 (Repose/8.5.0.0)",
            "1.0 Repose (Repose/7.3.5.0)",
            "1.1 Repose (Repose/7.1.3.2-SNAPSHOT)",
            "1.1 10.10.10.123:8080 (Repose/7.3.0.0)",
            "0.9 localhost:8080 (Repose/7.3.0.0)",
            "HTTP/1.1 LEGIT",
            "HTTP/2.0 pseudonym",
            $/HTTP/2.0 pseudonym (this, \(is\), a, comment)/$,
            "HTTP/2.0 pseudonym (yo (dawg (I heard (you (like) comments))))",
            "SIP/2.0/UDP 10.10.10.123:5060; branch=1234567890abc"]
    }
}
