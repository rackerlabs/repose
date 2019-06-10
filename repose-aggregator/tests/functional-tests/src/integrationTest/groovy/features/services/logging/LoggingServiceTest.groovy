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
package features.services.logging

import org.apache.http.HttpResponse
import org.apache.http.ProtocolVersion
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.HttpClients
import org.junit.Before
import org.junit.BeforeClass
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.server.CustomizableSocketServerConnector
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Endpoint
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Services
import spock.lang.Shared
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import static javax.servlet.http.HttpServletResponse.SC_OK
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.*
import static org.openrepose.commons.utils.string.Base64Helper.base64EncodeUtf8
import static org.rackspace.deproxy.Deproxy.REQUEST_ID_HEADER_NAME

@Category(Services)
class LoggingServiceTest extends ReposeValveTest {

    @Shared
    CustomizableSocketServerConnector socketServerConnector

    @BeforeClass
    def setupSpec() {
        deproxy = new Deproxy()
        Endpoint originService = deproxy.addEndpoint(name: 'origin service', connectorFactory: { Endpoint endpoint ->
            new CustomizableSocketServerConnector(endpoint, properties.targetPort)
        })
        socketServerConnector = originService.serverConnector as CustomizableSocketServerConnector

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/logging/common", params)
        repose.configurationProvider.applyConfigs("features/services/logging/good", params)
        repose.start()
        waitUntilReadyToServiceRequests()
    }

    @Before
    def setup() {
        reposeLogSearch.cleanLog()
    }

    def "Should log a static message"() {
        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
        )

        then: "Repose should return an OK (200)"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "The static message should have been logged"
        reposeLogSearch.awaitByString("INFO  default-static - Static Value")
    }

    @Unroll
    def "Should log a CLF message for #method"() {
        given:
        def userId = "UserId"
        def path = "/"
        def response = "<!DOCTYPE html><html><head><title>title</title></head><body></body></html>"

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            path: path,
            method: method,
            headers: [
                (USER_ID): userId
            ],
            defaultHandler: {
                return new Response(
                    code,
                    null,
                    null,
                    response
                )
            },
        )

        then: "Repose should return the code"
        messageChain.receivedResponse.code as Integer == code

        and: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "The common-log should have been logged"
        reposeLogSearch.awaitByString("INFO  common-log - 127.0.0.1 - $userId \\[\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z\\] \"$method $path HTTP/1.1\" $code ${response.length()}")

        and: "The written-log should have been logged"
        reposeLogSearch.awaitByString("INFO  written-log - outboundResponseBytesWritten=${response.length()}")

        where:
        method   | code
        "GET"    | 461
        "PUT"    | 462
        "POST"   | 463
        "PATCH"  | 464
        "DELETE" | 465
        "TRACE"  | 466
    }

    @Unroll
    def "Should log the #element"() {
        given:
        def path = "/modifyMe/$element"
        def query = "?query=orig"

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            path: path + query,
        )

        then: "Repose should return the code"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "The #element-log should have been logged"
        reposeLogSearch.awaitByString(message)

        where:
        element  | message
        "method" | "INFO  method-log - inboundRequestMethod=GET - outboundRequestMethod=OPTIONS"
        "path"   | "INFO  path-log - inboundRequestPath=/modifyMe/path - outboundRequestPath=/modifyMe/changed"
        "query"  | "INFO  query-log - inboundRequestQueryString=query=orig - outboundRequestQueryString=query=changed"
        "url"    | "INFO  url-log - inboundRequestUrl=$reposeEndpoint/modifyMe/url - outboundRequestUrl=http://new.url.com:\\d+/modifyMe/url"
    }


    def "Should log the time"() {
        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            path: "/modifyMe/time/",
            defaultHandler: { request ->
                Thread.sleep(1000)
                new Response(SC_OK)
            },
        )

        then: "Repose should return the code"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "The time-log should have been logged"
        reposeLogSearch.awaitByString("INFO  time-log - timeToHandleRequest=PT2.\\d+S - timeInOriginService=PT1.\\d+S")
    }

    def "Should log the traceId"() {
        given:
        def traceId = UUID.randomUUID().toString()

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            headers: [
                'X-Trans-Id': base64EncodeUtf8("""{"requestId":"$traceId","origin":null}""")
            ],
        )

        then: "Repose should return the code"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "The trace-log should have been logged"
        reposeLogSearch.awaitByString("INFO  trace-log - traceId=$traceId")
    }

    def "Should log the error"() {
        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            path: "/modifyMe/error/",
        )

        then: "Repose should return the code"
        messageChain.receivedResponse.code as Integer == 525

        and: "The request should NOT have reached the origin service"
        messageChain.handlings.size() == 0

        and: "The error-log should have been logged"
        reposeLogSearch.awaitByString("INFO  error-log - outboundResponseStatusCode=525 - outboundResponseReasonPhrase=Supercalifragilisticexpialidocious")
    }

    def "Should log an error even if Jetty rejects the request"() {
        HttpClient client = HttpClients.createDefault()
        HttpUriRequest request = new HttpGet(reposeEndpoint)
        request.setProtocolVersion(new ProtocolVersion("HTTP", 0, 0))

        when: "Request is sent through repose"
        HttpResponse response = client.execute(request)

        then: "Jetty should reject it"
        response.statusLine.statusCode == SC_BAD_REQUEST

        and: "The error-log should have been logged"
        reposeLogSearch.awaitByString("INFO  error-log - outboundResponseStatusCode=$SC_BAD_REQUEST - outboundResponseReasonPhrase=Unknown Version")
    }

    static def PROTOCOLS = [
        new ProtocolVersion("HTTP", 1, 0),
        new ProtocolVersion("HTTP", 1, 1),
    ]

    @Unroll
    def "Should log the protocols (#clientProtocol : #outboundProtocol : #originProtocol)"() {
        given: "the client will make a(n) #clientProtocol request"
        HttpClient client = HttpClients.createDefault()
        HttpUriRequest request = new HttpGet(reposeEndpoint + "/modifyMe/protocol/")
        request.setProtocolVersion(clientProtocol)

        and: "the origin service will return a(n) #originProtocol response"
        socketServerConnector.httpProtocol = originProtocol

        and: "Deproxy will track the request to the origin service"
        MessageChain messageChain = new MessageChain()
        String requestId = UUID.randomUUID().toString()
        deproxy.addMessageChain(requestId, messageChain)
        request.addHeader(REQUEST_ID_HEADER_NAME, requestId)
        request.addHeader("X-Protocol-Outbound", outboundProtocol.toString())

        when:
        HttpResponse response = client.execute(request)

        then: "Repose should return the code"
        response.statusLine.statusCode == SC_OK

        and: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "The protocol-log should have been logged"
        reposeLogSearch.awaitByString("INFO  protocol-log - inboundRequestProtocol=${clientProtocol.toString()} - outboundRequestProtocol=${outboundProtocol.toString()} - inboundResponseProtocol=${originProtocol.toString()}")

        where:
        [clientProtocol, outboundProtocol, originProtocol] << [PROTOCOLS, PROTOCOLS, PROTOCOLS].combinations()
    }

    def "Should log the headers"() {
        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            path: "/modifyMe/headers/",
            headers: [
                'X-Inbound-Request-Hdr': 'Inbound-Request-Val'
            ],
        )

        then: "Repose should return the code"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "The error-log should have been logged"
        reposeLogSearch.awaitByString("INFO  headers-log - inboundRequestHeaders=Inbound-Request-Val - outboundRequestHeaders=Outbound-Request-Val - outboundResponseHeaders=Outbound-Response-Val")
    }

    def "Should log the user/impersonator ID/name"() {
        given:
        def userId = UUID.randomUUID().toString()
        def userName = UUID.randomUUID().toString()
        def impersonatorUserId = UUID.randomUUID().toString()
        def impersonatorUserName = UUID.randomUUID().toString()

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            headers: [
                (USER_ID): userId,
                (USER_NAME): userName,
                (IMPERSONATOR_ID): impersonatorUserId,
                (IMPERSONATOR_NAME): impersonatorUserName,
            ],
        )

        then: "Repose should return an OK (200)"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "The escaped JSON should have been logged"
        reposeLogSearch.awaitByString("INFO  user-log - userId=$userId - userName=$userName - impersonatorUserId=$impersonatorUserId - impersonatorUserName=$impersonatorUserName")
    }

    def "Should log the local IP"() {
        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
        )

        then: "Repose should return an OK (200)"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "The escaped JSON should have been logged"
        reposeLogSearch.awaitByString(""""localIpAddress": "${InetAddress.loopbackAddress.hostAddress}",""")
    }

    def "Should log the host"() {
        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
        )

        then: "Repose should return an OK (200)"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "The escaped JSON should have been logged"
        reposeLogSearch.awaitByString(""""remoteHost": "${InetAddress.loopbackAddress.hostAddress}",""")
    }

    def "Should log the extensions"() {
        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            path: "/modifyMe/extensions/",
        )

        then: "Repose should return the code"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "The extensions-log should have been logged"
        reposeLogSearch.awaitByString("INFO  extensions-log - extensions=This is the value.")
    }

    def "Should log valid/escaped JSON"() {
        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            path: "/leaveMe/alone/",
        )

        then: "Repose should return an OK (200)"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "The escaped JSON should have been logged"
        // NOTE: Double escaping of the backslash is required since awaitByString is passed a RegEx.
        reposeLogSearch.awaitByString(""""requestLine": "GET \\\\/leaveMe\\\\/alone\\\\/ HTTP\\\\/1.1",""")
    }
}
