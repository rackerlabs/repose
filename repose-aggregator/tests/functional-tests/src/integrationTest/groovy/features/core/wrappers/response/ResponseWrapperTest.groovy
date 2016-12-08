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
package features.core.wrappers.response

import framework.ReposeValveTest
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

import javax.ws.rs.core.MediaType

class ResponseWrapperTest extends ReposeValveTest {
    static final String TEST_CASE_FIRST_FILTER_HEADER_NAME = "X-First-Filter-Test-Case"
    static final String TEST_CASE_SECOND_FILTER_HEADER_NAME = "X-Second-Filter-Test-Case"
    static final String RESPONSE_CODE_HEADER_NAME = "X-Test-Response-Code"
    static final String HEADER_MODE_HEADER_NAME = "X-Test-Header-Mode"
    static final String BODY_MODE_HEADER_NAME = "X-Test-Body-Mode"
    static final String REASON_HEADER_NAME = "X-Reason"
    static final String REASON_MESSAGE = "SOME REASON PHRASE"

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        def params = properties.getDefaultTemplateParams() + [
                testCaseFirstFilterHeaderName : TEST_CASE_FIRST_FILTER_HEADER_NAME,
                testCaseSecondFilterHeaderName: TEST_CASE_SECOND_FILTER_HEADER_NAME,
                responseCodeHeaderName        : RESPONSE_CODE_HEADER_NAME,
                headerModeHeaderName          : HEADER_MODE_HEADER_NAME,
                bodyModeHeaderName            : BODY_MODE_HEADER_NAME,
                reasonHeaderName              : REASON_HEADER_NAME,
                reasonMessage                 : REASON_MESSAGE]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/wrappers/response", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def 'the reason phrase is passed to the next wrapper and can be added to a header'() {
        given: "the first filter will get the reason set by the second filter and will set the X-Reason header with the value"
        def firstFilterTestCase = "REASON_TO_HEADER"

        and: "the second filter will call sendError on the response wrapper"
        def responseCode = 418
        def secondFilterTestCase = "SEND_ERROR_WITH_MESSAGE"

        and: "the headers are set to tell the Scripting filters what to do"
        def headers = [
                (TEST_CASE_FIRST_FILTER_HEADER_NAME): firstFilterTestCase,
                (TEST_CASE_SECOND_FILTER_HEADER_NAME): secondFilterTestCase,
                (RESPONSE_CODE_HEADER_NAME): responseCode]

        when: "any request is made"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers)

        then: "the origin service does not receive the request"
        messageChain.handlings.isEmpty()

        and: "the response code is correctly set"
        messageChain.receivedResponse.code as Integer == responseCode

        and: "the response received by the user should contain a reason header"
        messageChain.receivedResponse.headers.contains(REASON_HEADER_NAME)
        messageChain.receivedResponse.headers.getFirstValue(REASON_HEADER_NAME) == REASON_MESSAGE
    }

    @Unroll
    def "the response body is set to '#expectedBody' by RMS when using the response wrapper's sendError(Int, String) method with value #responseCode"() {
        given: "the first filter will call sendError on the response wrapper"
        def firstFilterTestCase = "SEND_ERROR_WITH_MESSAGE"

        and: "the second filter will be set to throw an exception because Repose should never get to it"
        def secondFilterTestCase = "THROW_EXCEPTION"

        and: "the headers are set to tell the Scripting filters what to do"
        def headers = [
                (TEST_CASE_FIRST_FILTER_HEADER_NAME): firstFilterTestCase,
                (TEST_CASE_SECOND_FILTER_HEADER_NAME): secondFilterTestCase,
                (RESPONSE_CODE_HEADER_NAME): responseCode]

        when: "any request is made"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers)

        then: "the origin service does not receive the request"
        messageChain.handlings.isEmpty()

        and: "the response code is correctly set"
        messageChain.receivedResponse.code as Integer == responseCode

        and: "the response received by the user should contain the message in the body"
        messageChain.receivedResponse.body as String == expectedBody

        and: "if we were expecting a body, the content-type should be set"
        !expectedBody || messageChain.receivedResponse.headers.contains(CommonHttpHeader.CONTENT_TYPE.toString())
        !expectedBody || messageChain.receivedResponse.headers.getFirstValue(CommonHttpHeader.CONTENT_TYPE.toString()) == MediaType.TEXT_PLAIN

        where:
        responseCode | expectedBody
        418          | ""
        413          | REASON_MESSAGE
        412          | "Message: $REASON_MESSAGE"
        410          | "Static Message"
    }

    @Unroll
    def "the response body is set to '#expectedBody' by RMS when using the response wrapper's sendError(Int) method with value #responseCode"() {
        given: "the first filter will call sendError on the response wrapper using just a response code but no reason message"
        def firstFilterTestCase = "SEND_ERROR_CODE_ONLY"

        and: "the second filter will be set to throw an exception because Repose should never get to it"
        def secondFilterTestCase = "THROW_EXCEPTION"

        and: "the headers are set to tell the Scripting filters what to do"
        def headers = [
                (TEST_CASE_FIRST_FILTER_HEADER_NAME): firstFilterTestCase,
                (TEST_CASE_SECOND_FILTER_HEADER_NAME): secondFilterTestCase,
                (RESPONSE_CODE_HEADER_NAME): responseCode]

        when: "any request is made"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers)

        then: "the origin service does not receive the request"
        messageChain.handlings.isEmpty()

        and: "the response code is correctly set"
        messageChain.receivedResponse.code as Integer == responseCode

        and: "the response received by the user should contain the expected body"
        messageChain.receivedResponse.body as String == expectedBody

        where:
        responseCode | expectedBody
        418          | ""
        413          | ""
        412          | "Message:"
        410          | "Static Message"
    }

    @Unroll
    def "a filter can use the response wrapper's sendError method when wrapping the response with response modes '#wrapperHeaderMode' for header and '#wrapperBodyMode' for body"() {
        given: "the first filter will wrap the response again with the desired modes"
        def responseCode = 413
        def firstFilterTestCase = "REWRAP_RESPONSE"

        and: "the second filter will be set to throw an exception because Repose should never get to it"
        def secondFilterTestCase = "THROW_EXCEPTION"

        and: "the headers are set to tell the Scripting filters what to do"
        def headers = [
                (TEST_CASE_FIRST_FILTER_HEADER_NAME): firstFilterTestCase,
                (TEST_CASE_SECOND_FILTER_HEADER_NAME): secondFilterTestCase,
                (RESPONSE_CODE_HEADER_NAME): responseCode,
                (HEADER_MODE_HEADER_NAME): wrapperHeaderMode,
                (BODY_MODE_HEADER_NAME): wrapperBodyMode]

        when: "any request is made"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers)

        then: "the origin service does not receive the request"
        messageChain.handlings.isEmpty()

        and: "the response code is correctly set"
        messageChain.receivedResponse.code as Integer == responseCode

        and: "the response received by the user should contain the message in the body with the correct content type"
        messageChain.receivedResponse.body as String == REASON_MESSAGE
        messageChain.receivedResponse.headers.contains(CommonHttpHeader.CONTENT_TYPE.toString())
        messageChain.receivedResponse.headers.getFirstValue(CommonHttpHeader.CONTENT_TYPE.toString()) == MediaType.TEXT_PLAIN

        where:
        wrapperHeaderMode | wrapperBodyMode
        "PASSTHROUGH"     | "PASSTHROUGH"
        "PASSTHROUGH"     | "READONLY"
        "PASSTHROUGH"     | "MUTABLE"
        "READONLY"        | "PASSTHROUGH"
        "READONLY"        | "READONLY"
        "READONLY"        | "MUTABLE"
        "MUTABLE"         | "PASSTHROUGH"
        "MUTABLE"         | "READONLY"
        "MUTABLE"         | "MUTABLE"
    }
}
