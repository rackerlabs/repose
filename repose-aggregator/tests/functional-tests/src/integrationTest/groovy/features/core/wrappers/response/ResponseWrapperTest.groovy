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

import org.openrepose.commons.utils.servlet.http.ResponseMode
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Ignore
import spock.lang.Unroll

import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType

class ResponseWrapperTest extends ReposeValveTest {
    static final String FIRST_FILTER_TEST_HEADER_NAME = "X-First-Filter-Test-Case"
    static final String SECOND_FILTER_TEST_HEADER_NAME = "X-Second-Filter-Test-Case"
    static final String RESPONSE_CODE_HEADER_NAME = "X-Test-Response-Code"
    static final String HEADER_MODE_HEADER_NAME = "X-Test-Header-Mode"
    static final String BODY_MODE_HEADER_NAME = "X-Test-Body-Mode"
    static final String REASON_HEADER_NAME = "X-Reason"
    static final String REASON_MESSAGE = "SOME REASON PHRASE"

    static final String TC_REASON_TO_HEADER = "REASON_TO_HEADER"
    static final String TC_THROW_EXCEPTION = "THROW_EXCEPTION"
    static final String TC_SEND_ERROR_WITH_MESSAGE = "SEND_ERROR_WITH_MESSAGE"
    static final String TC_SEND_ERROR_CODE_ONLY = "SEND_ERROR_CODE_ONLY"
    static final String TC_REWRAP_RESPONSE = "REWRAP_RESPONSE"
    static final String TC_CALL_NEXT_FILTER = "CALL_NEXT_FILTER"
    static final String TC_SEND_ERROR_UNCOMMIT_SEND_ERROR = "SEND_ERROR_UNCOMMIT_SEND_ERROR"
    static final String TC_SEND_ERROR_UNCOMMIT_SET_BODY = "SEND_ERROR_UNCOMMIT_SET_BODY"
    static final String TC_SET_BODY_SEND_ERROR = "SET_BODY_SEND_ERROR"
    static final String TC_SET_LARGE_BODY_SEND_ERROR = "SET_LARGE_BODY_SEND_ERROR"
    static final String TC_CALL_NEXT_FILTER_THEN_SEND_ERROR = "CALL_NEXT_FILTER_THEN_SEND_ERROR"
    static final String TC_OUTPUT_STREAM_SEND_ERROR = "OUTPUT_STREAM_SEND_ERROR"
    static final String TC_OUTPUT_STREAM_SET_BODY_SEND_ERROR = "OUTPUT_STREAM_SET_BODY_SEND_ERROR"
    static final String TC_SET_BODY = "SET_BODY"

    static final String SCRIPT_IMPORTS = """\
        |import org.openrepose.commons.utils.servlet.http.*
        |import java.io.ByteArrayOutputStream
        |import javax.servlet.ServletOutputStream
        |import javax.servlet.WriteListener
        |import javax.ws.rs.core.MediaType
        |""".stripMargin()

    static final String SCRIPT_CLASSES_FUNCTIONS = """\
        |class ByteArrayServletOutputStream extends ServletOutputStream {
        |    def baos = new ByteArrayOutputStream()
        |
        |    void write(int b) {
        |        baos.write(b)
        |    }
        |
        |    String toString() {
        |        baos.toString()
        |    }
        |
        |    boolean isReady() {
        |        true
        |    }
        |
        |    void setWriteListener(WriteListener wl) {
        |        // do nothing
        |    }
        |}
        |
        |def setBody = { responseWrapper, status, body ->
        |    responseWrapper.setStatus(status)
        |    responseWrapper.setContentType(MediaType.TEXT_PLAIN)
        |    responseWrapper.getOutputStream().print(body)
        |}
        |
        |def responseCodeToReturn = request.getHeader("$RESPONSE_CODE_HEADER_NAME") as Integer
        |""".stripMargin()

    static final String SCRIPT_TEST_CASES = """\
        |if (testCase == "$TC_REASON_TO_HEADER") {
        |    filterChain.doFilter(request, response)
        |
        |    String reason = response.getReason()
        |    if (reason) {
        |        response.addHeader("$REASON_HEADER_NAME", reason)
        |    }
        |} else if (testCase == "$TC_THROW_EXCEPTION") {
        |    throw new Exception("This test failed as it should not have reached this filter.")
        |}  else if (testCase == "$TC_SEND_ERROR_WITH_MESSAGE") {
        |    response.sendError(responseCodeToReturn, "$REASON_MESSAGE")
        |} else if (testCase == "$TC_SEND_ERROR_CODE_ONLY") {
        |    response.sendError(responseCodeToReturn)
        |} else if (testCase == "$TC_REWRAP_RESPONSE") {
        |    def headerMode = ResponseMode.valueOf(request.getHeader("$HEADER_MODE_HEADER_NAME"))
        |    def bodyMode = ResponseMode.valueOf(request.getHeader("$BODY_MODE_HEADER_NAME"))
        |
        |    def rewrappedResponse = new HttpServletResponseWrapper(response, headerMode, bodyMode)
        |
        |    rewrappedResponse.sendError(responseCodeToReturn, "$REASON_MESSAGE")
        |
        |    if (headerMode == ResponseMode.MUTABLE || bodyMode == ResponseMode.MUTABLE) {
        |        rewrappedResponse.commitToResponse()
        |    }
        |} else if (testCase == "$TC_CALL_NEXT_FILTER") {
        |    filterChain.doFilter(request, response)
        |} else if (testCase == "$TC_SEND_ERROR_UNCOMMIT_SEND_ERROR") {
        |    response.sendError(499, "This message should not make it to the client.")
        |    response.uncommit()
        |    response.sendError(responseCodeToReturn, "$REASON_MESSAGE")
        |} else if (testCase == "$TC_SEND_ERROR_UNCOMMIT_SET_BODY") {
        |    response.sendError(499, "This message should not make it to the client.")
        |    response.uncommit()
        |    response.resetError()
        |    setBody(response, responseCodeToReturn, "$REASON_MESSAGE")
        |} else if (testCase == "$TC_SET_BODY_SEND_ERROR") {
        |    setBody(response, 201, "This message should not make it to the client.")
        |    response.sendError(responseCodeToReturn, "$REASON_MESSAGE")
        |} else if (testCase == "$TC_SET_LARGE_BODY_SEND_ERROR") {
        |    setBody(response, 201, "This message should not make it to the client." * 1_000_000)
        |    response.sendError(responseCodeToReturn, "$REASON_MESSAGE")
        |} else if (testCase == "$TC_CALL_NEXT_FILTER_THEN_SEND_ERROR") {
        |    filterChain.doFilter(request, response)
        |    response.sendError(responseCodeToReturn, "$REASON_MESSAGE")
        |} else if (testCase == "$TC_OUTPUT_STREAM_SEND_ERROR") {
        |    def out = new ByteArrayServletOutputStream()
        |    def rewrappedResponse = new HttpServletResponseWrapper(response, ResponseMode.MUTABLE, ResponseMode.MUTABLE, out)
        |
        |    rewrappedResponse.sendError(responseCodeToReturn, "$REASON_MESSAGE")
        |    rewrappedResponse.commitToResponse()
        |} else if (testCase == "$TC_OUTPUT_STREAM_SET_BODY_SEND_ERROR") {
        |    def out = new ByteArrayServletOutputStream()
        |    def rewrappedResponse = new HttpServletResponseWrapper(response, ResponseMode.MUTABLE, ResponseMode.MUTABLE, out)
        |
        |    setBody(rewrappedResponse, 201, "This message should not make it to the client.")
        |
        |    rewrappedResponse.sendError(responseCodeToReturn, "$REASON_MESSAGE")
        |    rewrappedResponse.commitToResponse()
        |} else if (testCase == "$TC_SET_BODY") {
        |    setBody(response, 201, "This message should not make it to the client.")
        |}
        |""".stripMargin()

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        def params = properties.getDefaultTemplateParams() + [
                testCaseFirstFilterHeaderName : FIRST_FILTER_TEST_HEADER_NAME,
                testCaseSecondFilterHeaderName: SECOND_FILTER_TEST_HEADER_NAME,
                scriptImports                 : SCRIPT_IMPORTS,
                scriptClassesFunctions        : SCRIPT_CLASSES_FUNCTIONS,
                scriptTestCases               : SCRIPT_TEST_CASES]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/wrappers/response", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def 'the reason message is passed to the original wrapper and can be added to a header'() {
        given: "the first filter will get the reason set by the second filter and will set the X-Reason header with the value"
        def firstFilterTestCase = TC_REASON_TO_HEADER

        and: "the second filter will call sendError on the response wrapper"
        def responseCode = 418
        def secondFilterTestCase = TC_SEND_ERROR_WITH_MESSAGE

        and: "the headers are set to tell the Scripting filters what to do"
        def headers = [
                (FIRST_FILTER_TEST_HEADER_NAME) : firstFilterTestCase,
                (SECOND_FILTER_TEST_HEADER_NAME): secondFilterTestCase,
                (RESPONSE_CODE_HEADER_NAME)     : responseCode]

        when: "any request is made"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers)

        then: "the origin service does not receive the request"
        messageChain.handlings.isEmpty()

        and: "the response code is correctly set"
        messageChain.receivedResponse.code as Integer == responseCode

        and: "the response received by the user should contain a reason header"
        messageChain.receivedResponse.headers.contains(REASON_HEADER_NAME)
        messageChain.receivedResponse.headers.getFirstValue(REASON_HEADER_NAME) == REASON_MESSAGE

        and: "since the second filter sends an error the Response is closed and additional attempts to add a header should be logged as they may be ignored"
        reposeLogSearch.searchByString("Calls to .*addHeader.* after the response has been committed may be ignored -- the following header may not be modified: $REASON_HEADER_NAME: $REASON_MESSAGE").size() > 0
    }

    def "the reason message is set on the response status line"() {
        given: "the first filter will call sendError on the response wrapper"
        def responseCode = 418
        def firstFilterTestCase = TC_SEND_ERROR_WITH_MESSAGE

        and: "the second filter will be set to throw an exception because Repose should never get to it"
        def secondFilterTestCase = TC_THROW_EXCEPTION

        and: "the headers are set to tell the Scripting filters what to do"
        def headers = [
            (FIRST_FILTER_TEST_HEADER_NAME) : firstFilterTestCase,
            (SECOND_FILTER_TEST_HEADER_NAME): secondFilterTestCase,
            (RESPONSE_CODE_HEADER_NAME)     : responseCode]

        when: "any request is made"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers)

        then: "the origin service does not receive the request"
        messageChain.handlings.isEmpty()

        and: "the response code is correctly set"
        messageChain.receivedResponse.code as Integer == responseCode

        and: "the response received by the user should contain the message in the body"
        messageChain.receivedResponse.message == REASON_MESSAGE
    }

    @Ignore('RMS is not currently supported')
    @Unroll
    def "the response body is set to '#expectedBody' by RMS when using the response wrapper's sendError(Int, String) method with value #responseCode"() {
        given: "the first filter will call sendError on the response wrapper"
        def firstFilterTestCase = TC_SEND_ERROR_WITH_MESSAGE

        and: "the second filter will be set to throw an exception because Repose should never get to it"
        def secondFilterTestCase = TC_THROW_EXCEPTION

        and: "the headers are set to tell the Scripting filters what to do"
        def headers = [
                (FIRST_FILTER_TEST_HEADER_NAME) : firstFilterTestCase,
                (SECOND_FILTER_TEST_HEADER_NAME): secondFilterTestCase,
                (RESPONSE_CODE_HEADER_NAME)     : responseCode]

        when: "any request is made"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers)

        then: "the origin service does not receive the request"
        messageChain.handlings.isEmpty()

        and: "the response code is correctly set"
        messageChain.receivedResponse.code as Integer == responseCode

        and: "the response received by the user should contain the message in the body"
        messageChain.receivedResponse.body as String == expectedBody

        and: "if we were expecting a body, the content-type should be set"
        !expectedBody || messageChain.receivedResponse.headers.contains(HttpHeaders.CONTENT_TYPE)
        !expectedBody || messageChain.receivedResponse.headers.getFirstValue(HttpHeaders.CONTENT_TYPE) == MediaType.TEXT_PLAIN

        where:
        responseCode | expectedBody
        418          | ""
        413          | REASON_MESSAGE
        412          | "Message: $REASON_MESSAGE"
        411          | "Static Message"
    }

    @Ignore('RMS is not currently supported')
    @Unroll
    def "the response body is set to '#expectedBody' by RMS when using the response wrapper's sendError(Int) method with value #responseCode"() {
        given: "the first filter will call sendError on the response wrapper using just a response code but no reason message"
        def firstFilterTestCase = TC_SEND_ERROR_CODE_ONLY

        and: "the second filter will be set to throw an exception because Repose should never get to it"
        def secondFilterTestCase = TC_THROW_EXCEPTION

        and: "the headers are set to tell the Scripting filters what to do"
        def headers = [
                (FIRST_FILTER_TEST_HEADER_NAME) : firstFilterTestCase,
                (SECOND_FILTER_TEST_HEADER_NAME): secondFilterTestCase,
                (RESPONSE_CODE_HEADER_NAME)     : responseCode]

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
        411          | "Static Message"
    }

    def "the response body is empty when send error is called after the body is set and response messaging does not rewrite"() {
        given: "a response code not handled by response messages"
        def responseCode = 418

        and: "the first filter will set the body then call sendError on the response wrapper using just a response code but no reason message"
        def firstFilterTestCase = TC_SET_BODY_SEND_ERROR

        and: "the second filter will be set to throw an exception because Repose should never get to it"
        def secondFilterTestCase = TC_THROW_EXCEPTION

        and: "the headers are set to tell the Scripting filters what to do"
        def headers = [
            (FIRST_FILTER_TEST_HEADER_NAME) : firstFilterTestCase,
            (SECOND_FILTER_TEST_HEADER_NAME): secondFilterTestCase,
            (RESPONSE_CODE_HEADER_NAME)     : responseCode]

        when: "any request is made"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers)

        then: "the origin service does not receive the request"
        messageChain.handlings.isEmpty()

        and: "the response code is correctly set"
        messageChain.receivedResponse.code as Integer == responseCode

        and: "the response received by the user should be empty without a content type"
        (messageChain.receivedResponse.body as String).isEmpty()
        !messageChain.receivedResponse.headers.contains(HttpHeaders.CONTENT_TYPE)
        messageChain.receivedResponse.headers.contains(HttpHeaders.CONTENT_LENGTH)
        messageChain.receivedResponse.headers.getFirstValue(HttpHeaders.CONTENT_LENGTH) as int == 0
    }

    @Unroll
    def "a filter can use the response wrapper's sendError method when wrapping the response with response modes '#wrapperHeaderMode' for header and '#wrapperBodyMode' for body"() {
        given: "the first filter will wrap the response again with the desired modes"
        def responseCode = 413
        def firstFilterTestCase = TC_REWRAP_RESPONSE

        and: "the second filter will be set to throw an exception because Repose should never get to it"
        def secondFilterTestCase = TC_THROW_EXCEPTION

        and: "the headers are set to tell the Scripting filters what to do"
        def headers = [
                (FIRST_FILTER_TEST_HEADER_NAME) : firstFilterTestCase,
                (SECOND_FILTER_TEST_HEADER_NAME): secondFilterTestCase,
                (RESPONSE_CODE_HEADER_NAME)     : responseCode,
                (HEADER_MODE_HEADER_NAME)       : wrapperHeaderMode,
                (BODY_MODE_HEADER_NAME)         : wrapperBodyMode]

        when: "any request is made"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers)

        then: "the origin service does not receive the request"
        messageChain.handlings.isEmpty()

        and: "the response code is correctly set"
        messageChain.receivedResponse.code as Integer == responseCode

        and: "the response received by the user should contain the message in the status line with the correct content type"
        messageChain.receivedResponse.message == REASON_MESSAGE
        // todo: uncomment these checks when RMS is supported
        // messageChain.receivedResponse.body as String == REASON_MESSAGE
        // messageChain.receivedResponse.headers.contains(HttpHeaders.CONTENT_TYPE)
        // messageChain.receivedResponse.headers.getFirstValue(HttpHeaders.CONTENT_TYPE) == MediaType.TEXT_PLAIN

        where:
        [wrapperHeaderMode, wrapperBodyMode] <<
                [ResponseMode.values()*.toString(), ResponseMode.values()*.toString()].combinations()
    }

    @Unroll
    def "should be able to #testDescription"() {
        given: "the headers are set to tell the Scripting filters what to do"
        def headers = [
                (FIRST_FILTER_TEST_HEADER_NAME) : firstFilterTestCase,
                (SECOND_FILTER_TEST_HEADER_NAME): secondFilterTestCase,
                (RESPONSE_CODE_HEADER_NAME)     : responseCode]

        when: "any request is made"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers)

        then: "the origin service does not receive the request"
        messageChain.handlings.isEmpty()

        and: "the response code is correctly set"
        messageChain.receivedResponse.code as Integer == responseCode

        and: "the response received by the user should contain the message in the body with the correct content type"
        if (bodyMessage) {
            assert messageChain.receivedResponse.body as String == REASON_MESSAGE
            assert messageChain.receivedResponse.headers.contains(HttpHeaders.CONTENT_TYPE)
            assert messageChain.receivedResponse.headers.getFirstValue(HttpHeaders.CONTENT_TYPE) == MediaType.TEXT_PLAIN
        }

        and: "the response received by the user should contain the message in the status line"
        if (statusMessage) {
            assert messageChain.receivedResponse.message == REASON_MESSAGE
        }

        where:
        firstFilterTestCase                  | secondFilterTestCase              | responseCode || bodyMessage | statusMessage | testDescription
        TC_SEND_ERROR_UNCOMMIT_SEND_ERROR    | TC_THROW_EXCEPTION                | 413          || false       | true          | "sendError, uncommit, sendError with another wrapper as the original wrapper"
        TC_CALL_NEXT_FILTER                  | TC_SEND_ERROR_UNCOMMIT_SEND_ERROR | 413          || false       | true          | "sendError, uncommit, sendError with our wrapper as the original wrapper"
        TC_SEND_ERROR_UNCOMMIT_SET_BODY      | TC_THROW_EXCEPTION                | 200          || true        | false         | "sendError, uncommit/resetError, set the body with another wrapper as the original wrapper"
        TC_CALL_NEXT_FILTER                  | TC_SEND_ERROR_UNCOMMIT_SET_BODY   | 200          || true        | false         | "sendError, uncommit/resetError, set the body with our wrapper as the original wrapper"
        TC_SET_BODY_SEND_ERROR               | TC_THROW_EXCEPTION                | 413          || false       | true          | "set the body then sendError with another wrapper as the original wrapper"
        TC_CALL_NEXT_FILTER                  | TC_SET_BODY_SEND_ERROR            | 413          || false       | true          | "set the body then sendError with out wrapper as the original wrapper"
        TC_SET_LARGE_BODY_SEND_ERROR         | TC_THROW_EXCEPTION                | 413          || false       | true          | "set a large body then sendError with another wrapper as the original wrapper"
        TC_CALL_NEXT_FILTER                  | TC_SET_LARGE_BODY_SEND_ERROR      | 413          || false       | true          | "set a large body then sendError with out wrapper as the original wrapper"
        TC_CALL_NEXT_FILTER_THEN_SEND_ERROR  | TC_SET_BODY                       | 413          || false       | true          | "call next filter, it sets the body, then the first filter calls sendError"
        TC_OUTPUT_STREAM_SEND_ERROR          | TC_THROW_EXCEPTION                | 413          || false       | true          | "rewrap the response with a new output stream then sendError"
        TC_OUTPUT_STREAM_SET_BODY_SEND_ERROR | TC_THROW_EXCEPTION                | 413          || false       | true          | "rewrap the response with a new output stream, set the body, then sendError"
    }
}
