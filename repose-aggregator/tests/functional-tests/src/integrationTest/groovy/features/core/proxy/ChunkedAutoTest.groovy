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

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import scaffold.category.Core
import spock.lang.Unroll

import static org.springframework.http.HttpHeaders.*
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE

@Category(Core)
class ChunkedAutoTest extends ReposeValveTest {

    private static final String TEST_BODY = "Test body string"

    // These HTTP request methods are allowed to contain a body.
    private static final List<String> BODY_METHODS = ["POST", "PUT", "DELETE", "PATCH"]

    // These HTTP request methods are not allowed to contain a body.
    // Note that this constraint is imposed by Repose for all methods other than TRACE.
    // The TRACE method is not allowed to contain a body according to HTTP specification.
    private static final List<String> NO_BODY_METHODS = ["GET", "HEAD", "OPTIONS", "TRACE"]

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy", params)
        repose.configurationProvider.applyConfigs("features/core/proxy/chunkedauto", params)
        repose.start()
    }

    @Unroll
    def "when the client #method request #clientRequestBodyDescription, then the origin request should #originRequestBodyDescription"() {
        when: "the client makes a request to Repose"
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            method: method,
            headers: [(CONTENT_TYPE): TEXT_PLAIN_VALUE],
            requestBody: body,
            chunked: chunked
        )

        and: "the client and origin request headers are captured"
        Request clientRequest = messageChain.sentRequest
        Request originRequest = messageChain.handlings[0].request

        then: "the client request meets expectations"
        clientRequest.headers.contains(TRANSFER_ENCODING) == (body && chunked)
        clientRequest.headers.contains(CONTENT_LENGTH) == (body && !chunked)
        clientRequest.body == (body ?: "")

        and: "the origin request meets expectations"
        originRequest.headers.contains(CONTENT_TYPE)
        originRequest.headers.contains(TRANSFER_ENCODING) == hasTransferEncoding
        originRequest.headers[TRANSFER_ENCODING]?.equalsIgnoreCase("chunked") as boolean == hasTransferEncoding
        originRequest.headers.contains(CONTENT_LENGTH) == hasContentLength
        originRequest.headers[CONTENT_LENGTH] == (hasContentLength ? TEST_BODY.length() as String : null)
        new String(originRequest.body) == (BODY_METHODS.contains(method) ? (body ?: "") : "")

        where:
        [method, body, chunked] << [BODY_METHODS + NO_BODY_METHODS, [TEST_BODY, null], [true, false]].combinations()
        hasTransferEncoding = BODY_METHODS.contains(method) && body && chunked
        hasContentLength = BODY_METHODS.contains(method) && body && !chunked

        // Generated wording for the test name
        clientRequestBodyDescription = body ? "has a ${chunked ? "chunked" : "non-chunked"} body" : "does not have a body"
        originRequestBodyDescription = (BODY_METHODS.contains(method) && body) ? "have a ${chunked ? "chunked" : "non-chunked"} body" : "not have a body"
    }
}
