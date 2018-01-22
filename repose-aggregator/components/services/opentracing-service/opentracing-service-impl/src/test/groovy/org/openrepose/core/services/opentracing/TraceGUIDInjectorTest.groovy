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
package org.openrepose.core.services.opentracing

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.codec.binary.Base64
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.springframework.mock.web.MockHttpServletRequest

import javax.servlet.http.HttpServletRequest

import static org.junit.Assert.*

class TraceGUIDInjectorTest {

    public static final String SPAN_KEY = "span-id"
    final String TRANS_ID_HEADER = "x-trans-id"

    Map<String, String> headerMap
    HttpServletRequest servletRequest
    HttpServletRequestWrapper requestWrapper

    @Before
    void setUp() {
        headerMap = new HashMap<>()
        headerMap.put("content-type", "application/json")
        headerMap.put("accept", "application/xml")
        String traceHeaderValue = new String(
                Base64.encodeBase64("{\"id\":12345,\"origin\":null}".bytes))
        headerMap.put(TRANS_ID_HEADER, traceHeaderValue)

        servletRequest = new MockHttpServletRequest()
        requestWrapper = new HttpServletRequestWrapper(servletRequest)
        requestWrapper.addHeader("content-type", "application/json")
        requestWrapper.addHeader("accept", "application/xml")
        requestWrapper.addHeader(TRANS_ID_HEADER, traceHeaderValue)

    }

    @After
    void tearDown() {
        headerMap.clear()
        for (String header : requestWrapper.headerNamesList)
            requestWrapper.removeHeader(header)
    }

    @Test
    void testConstructorWithHeaders() {
        headerMap = new HashMap<>()

        TraceGUIDInjector traceGUIDInjector = new TraceGUIDInjector(
                headerMap, TRANS_ID_HEADER)
        assertEquals("Validate header map is set",
                traceGUIDInjector.headers, headerMap)
        assertEquals("Validate tracing header is set",
                traceGUIDInjector.tracerHeader, "x-trans-id")

    }

    @Test
    void testConstructorWithServletWrapper() {
        TraceGUIDInjector traceGUIDInjector = new TraceGUIDInjector(
                requestWrapper, TRANS_ID_HEADER)

        assertEquals("Validate request wrapper is set",
                traceGUIDInjector.httpServletRequestWrapper, requestWrapper)
        assertEquals("Validate tracing header is set",
                traceGUIDInjector.tracerHeader, "x-trans-id")

    }

    @Test(expected = UnsupportedOperationException.class)
    void testIteratorThrowsInvalidOperation() {
        TraceGUIDInjector traceGUIDInjector = new TraceGUIDInjector(
                headerMap, TRANS_ID_HEADER)
        traceGUIDInjector.iterator()
    }

    @Test
    void testPutNullWrapperAndHeaders() {
        TraceGUIDInjector traceGUIDInjector = new TraceGUIDInjector(null, TRANS_ID_HEADER)

        traceGUIDInjector.put("key", "value")

        assertNull("Validate request wrapper is null",
                traceGUIDInjector.httpServletRequestWrapper)

        assertNull("Validate headers are null",
                traceGUIDInjector.headers)
    }

    @Test
    void testPutWrapperAndNullTracingHeader() {
        TraceGUIDInjector traceGUIDInjector = new TraceGUIDInjector(
                requestWrapper, null)

        traceGUIDInjector.put(SPAN_KEY, "new-value")

        assertNull("Validate tracing header is null",
                traceGUIDInjector.tracerHeader)

        assertEquals("Validate request wrapper still has 3 headers",
                traceGUIDInjector.httpServletRequestWrapper.headerNamesList.size(), 3)

        assertEquals("Validate header content-type is set",
                traceGUIDInjector.httpServletRequestWrapper.getHeader(
                        "content-type"), "application/json")

        assertEquals("Validate header accept is set",
                traceGUIDInjector.httpServletRequestWrapper.getHeader(
                        "accept"), "application/xml")

        validateSpanWasNotAdded(traceGUIDInjector.httpServletRequestWrapper.getHeader(
                TRANS_ID_HEADER))
    }

    @Test
    void testPutHeadersAndNullTracingHeader() {
        TraceGUIDInjector traceGUIDInjector = new TraceGUIDInjector(
                headerMap, null)

        traceGUIDInjector.put(SPAN_KEY, "new-value")

        assertEquals("Validate request wrapper still has 3 headers",
                traceGUIDInjector.headers.size(), 3)

        assertEquals("Validate header content-type is set",
                traceGUIDInjector.headers.get("content-type"), "application/json")

        assertEquals("Validate header accept is set",
                traceGUIDInjector.headers.get("accept"), "application/xml")


        validateSpanWasNotAdded(traceGUIDInjector.headers.get(
                TRANS_ID_HEADER))
    }

    @Test
    void testPutHeadersKeyIsNull() {
        TraceGUIDInjector traceGUIDInjector = new TraceGUIDInjector(headerMap, TRANS_ID_HEADER)

        traceGUIDInjector.put(null, "value")

        assertNull("Validate request wrapper is null",
                traceGUIDInjector.httpServletRequestWrapper)

        assertEquals("Validate there are still 3 header entries",
                traceGUIDInjector.headers.size(), 3)

        validateSpanWasNotAdded(traceGUIDInjector.headers.get(
                TRANS_ID_HEADER))
    }

    @Test
    void testPutHeadersValueIsNull() {
        TraceGUIDInjector traceGUIDInjector = new TraceGUIDInjector(headerMap, TRANS_ID_HEADER)

        traceGUIDInjector.put(SPAN_KEY, null)

        assertNull("Validate request wrapper is null",
                traceGUIDInjector.httpServletRequestWrapper)

        assertEquals("Validate there are still 3 header entries",
                traceGUIDInjector.headers.size(), 3)

        validateSpanWasNotAdded(traceGUIDInjector.headers.get(
                TRANS_ID_HEADER))

    }


    @Test
    void testPutWrapperKeyIsNull() {
        TraceGUIDInjector traceGUIDInjector = new TraceGUIDInjector(requestWrapper, TRANS_ID_HEADER)

        traceGUIDInjector.put(null, "value")

        assertNull("Validate headers map is null",
                traceGUIDInjector.headers)

        assertEquals("Validate there are still 3 header entries",
                traceGUIDInjector.httpServletRequestWrapper.headerNamesList.size(), 3)

        validateSpanWasNotAdded(traceGUIDInjector.httpServletRequestWrapper.getHeader(
                TRANS_ID_HEADER))
    }

    @Test
    void testPutWrapperValueIsNull() {
        TraceGUIDInjector traceGUIDInjector = new TraceGUIDInjector(requestWrapper, TRANS_ID_HEADER)

        traceGUIDInjector.put(SPAN_KEY, null)

        assertNull("Validate headers map is null",
                traceGUIDInjector.headers)

        assertEquals("Validate there are still 3 header entries",
                traceGUIDInjector.httpServletRequestWrapper.headerNamesList.size(), 3)

        validateSpanWasNotAdded(traceGUIDInjector.httpServletRequestWrapper.getHeader(
                TRANS_ID_HEADER))
    }

    @Test
    void testPutHeadersTracingHeaderEmpty() {
        headerMap.put(TRANS_ID_HEADER, null)

        TraceGUIDInjector traceGUIDInjector = new TraceGUIDInjector(headerMap, TRANS_ID_HEADER)

        traceGUIDInjector.put(SPAN_KEY, "value")

        assertNull("Validate request wrapper is null",
                traceGUIDInjector.httpServletRequestWrapper)

        assertEquals("Validate there are still 3 header entries",
                traceGUIDInjector.headers.size(), 3)

        assertNull("Validate trace header is still null",
                headerMap.get(TRANS_ID_HEADER))

    }

    @Test
    void testPutWrapperTracingHeaderEmpty() {
        requestWrapper.replaceHeader(TRANS_ID_HEADER, null)

        TraceGUIDInjector traceGUIDInjector = new TraceGUIDInjector(requestWrapper, TRANS_ID_HEADER)

        traceGUIDInjector.put(SPAN_KEY, "value")

        assertNull("Validate headers map is null",
                traceGUIDInjector.headers)

        assertEquals("Validate there are still 3 header entries",
                traceGUIDInjector.httpServletRequestWrapper.headerNamesList.size(), 3)

        assertNull("Validate trace header is still null",
                traceGUIDInjector.httpServletRequestWrapper.getHeader(TRANS_ID_HEADER))

    }

    @Test
    void testPutHeadersTracingHeaderInvalidBase64() {
        headerMap.put(TRANS_ID_HEADER, "somethingshady")

        TraceGUIDInjector traceGUIDInjector = new TraceGUIDInjector(headerMap, TRANS_ID_HEADER)

        traceGUIDInjector.put(SPAN_KEY, "value")

        assertNull("Validate request wrapper is null",
                traceGUIDInjector.httpServletRequestWrapper)

        assertEquals("Validate there are still 3 header entries",
                traceGUIDInjector.headers.size(), 3)

        assertEquals("Validate trace header is still set to whatever it was before",
                headerMap.get(TRANS_ID_HEADER), "somethingshady")

    }

    @Test
    void testPutWrapperTracingHeaderInvalidBase64() {
        requestWrapper.replaceHeader(TRANS_ID_HEADER, "somethingshady")

        TraceGUIDInjector traceGUIDInjector = new TraceGUIDInjector(requestWrapper, TRANS_ID_HEADER)

        traceGUIDInjector.put(SPAN_KEY, "value")

        assertNull("Validate headers map is null",
                traceGUIDInjector.headers)

        assertEquals("Validate there are still 3 header entries",
                traceGUIDInjector.httpServletRequestWrapper.headerNamesList.size(), 3)

        assertEquals("Validate trace header is still set to whatever it was before",
                traceGUIDInjector.httpServletRequestWrapper.getHeader(TRANS_ID_HEADER), "somethingshady")

    }

    @Test
    void testPutHeadersTracingHeaderValidBase64AndExistingSPANKey() {
        String traceHeaderValue = new String(
                Base64.encodeBase64(
                        String.format(
                                "{\"id\":12345,\"origin\":null,\"%s\":\"current-value\"}", SPAN_KEY).bytes))
        headerMap.put(TRANS_ID_HEADER, traceHeaderValue)

        TraceGUIDInjector traceGUIDInjector = new TraceGUIDInjector(headerMap, TRANS_ID_HEADER)

        traceGUIDInjector.put(SPAN_KEY, "new-value")

        assertNull("Validate request wrapper is null",
                traceGUIDInjector.httpServletRequestWrapper)

        assertEquals("Validate there are still 3 header entries",
                traceGUIDInjector.headers.size(), 3)

        validateSpanWasAdded(traceGUIDInjector.headers.get(
                TRANS_ID_HEADER), "new-value")

    }

    @Test
    void testPutWrapperTracingHeaderValidBase64AndExistingSPANKey() {
        String traceHeaderValue = new String(
                Base64.encodeBase64(
                        String.format(
                                "{\"id\":12345,\"origin\":null,\"%s\":\"current-value\"}", SPAN_KEY).bytes))
        requestWrapper.replaceHeader(TRANS_ID_HEADER, traceHeaderValue)

        TraceGUIDInjector traceGUIDInjector = new TraceGUIDInjector(requestWrapper, TRANS_ID_HEADER)

        traceGUIDInjector.put(SPAN_KEY, "new-value")

        assertNull("Validate headers map is null",
                traceGUIDInjector.headers)

        assertEquals("Validate there are still 3 header entries",
                traceGUIDInjector.httpServletRequestWrapper.headerNamesList.size(), 3)

        validateSpanWasAdded(traceGUIDInjector.httpServletRequestWrapper.getHeader(
                TRANS_ID_HEADER), "new-value")
    }


    @Test
    void testPutHeadersTracingHeaderValidBase64AndNewSPANKey() {
        TraceGUIDInjector traceGUIDInjector = new TraceGUIDInjector(headerMap, TRANS_ID_HEADER)

        traceGUIDInjector.put(SPAN_KEY, "new-value")

        assertNull("Validate request wrapper is null",
                traceGUIDInjector.httpServletRequestWrapper)

        assertEquals("Validate there are still 3 header entries",
                traceGUIDInjector.headers.size(), 3)

        validateSpanWasAdded(traceGUIDInjector.headers.get(
                TRANS_ID_HEADER), "new-value")

    }

    @Test
    void testPutWrapperTracingHeaderValidBase64AndNewSPANKey() {
        TraceGUIDInjector traceGUIDInjector = new TraceGUIDInjector(requestWrapper, TRANS_ID_HEADER)

        traceGUIDInjector.put(SPAN_KEY, "new-value")

        assertNull("Validate headers map is null",
                traceGUIDInjector.headers)

        assertEquals("Validate there are still 3 header entries",
                traceGUIDInjector.httpServletRequestWrapper.headerNamesList.size(), 3)

        validateSpanWasAdded(traceGUIDInjector.httpServletRequestWrapper.getHeader(
                TRANS_ID_HEADER), "new-value")
    }


    private void validateSpanWasNotAdded(String traceHeaderValue) {
        ObjectMapper mapper = new ObjectMapper()
        Map<String, String> tracingMap = mapper.readValue(
                Base64.decodeBase64(traceHeaderValue), new TypeReference<Map<String, String>>() {
        })

        assertFalse("Validate span key is not set in tracing header",
                tracingMap.containsKey(SPAN_KEY))

        assertEquals("Validate other keys are still set in tracing header",
                tracingMap.size(), 2)

    }

    private void validateSpanWasAdded(String traceHeaderValue, String spanValue) {
        ObjectMapper mapper = new ObjectMapper()
        Map<String, String> tracingMap = mapper.readValue(
                Base64.decodeBase64(traceHeaderValue), new TypeReference<Map<String, String>>() {
        })

        assertTrue("Validate span key is set in tracing header",
                tracingMap.containsKey(SPAN_KEY))

        assertEquals("Validate other keys are still set in tracing header",
                tracingMap.size(), 3)

        assertEquals("Validate span key is set to new value",
                tracingMap.get(SPAN_KEY), spanValue)

    }

}
