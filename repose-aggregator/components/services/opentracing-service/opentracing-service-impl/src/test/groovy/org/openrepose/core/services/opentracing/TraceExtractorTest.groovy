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

import org.apache.commons.codec.binary.Base64
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.springframework.mock.web.MockHttpServletRequest

import javax.servlet.http.HttpServletRequest

import static org.junit.Assert.*


class TraceExtractorTest {

    public static final String SPAN_KEY = "span-id"
    final String TRANS_ID_HEADER = "x-trans-id"

    final String CONTENT_TYPE = "content-type"
    final String ACCEPT = "accept"

    Map<String, String> headerMap
    HttpServletRequest servletRequest
    HttpServletRequestWrapper requestWrapper

    @Before
    void setUp() {
        headerMap = new HashMap<>()
        headerMap.put(CONTENT_TYPE, "application/json")
        headerMap.put(ACCEPT, "application/xml")
        String traceHeaderValue = new String(
            Base64.encodeBase64("{\"id\":12345,\"origin\":null}".bytes))
        headerMap.put(TRANS_ID_HEADER, traceHeaderValue)

        servletRequest = new MockHttpServletRequest()
        requestWrapper = new HttpServletRequestWrapper(servletRequest)
        requestWrapper.addHeader(CONTENT_TYPE, "application/json")
        requestWrapper.addHeader(ACCEPT, "application/xml")
        requestWrapper.addHeader(TRANS_ID_HEADER, traceHeaderValue)

    }

    @After
    void tearDown() {
        headerMap.clear()
        for (String header : requestWrapper.headerNamesList)
            requestWrapper.removeHeader(header)
    }

    @Test
    void testConstructorWithEmptyWrapper() {
        requestWrapper = new HttpServletRequestWrapper(servletRequest)

        TracerExtractor tracerExtractor = new TracerExtractor(
                requestWrapper)
        assertTrue("Validate header map was not populated",
                tracerExtractor.headers.isEmpty())

    }

    @Test
    void testConstructorWithHeaders() {
        TracerExtractor tracerExtractor = new TracerExtractor(
            requestWrapper)
        assertEquals("Validate header map is populated",
            tracerExtractor.headers.size(), 3)
        assertEquals(tracerExtractor.headers.get(CONTENT_TYPE).size(), 1)
        assertEquals(tracerExtractor.headers.get(ACCEPT).size(), 1)
        assertEquals(tracerExtractor.headers.get(TRANS_ID_HEADER).size(), 1)

    }

    @Test
    void testConstructorWithMultiValueHeaders() {
        requestWrapper.addHeader(ACCEPT, "application/xml")

        TracerExtractor tracerExtractor = new TracerExtractor(
            requestWrapper)
        assertEquals("Validate header map is populated",
            tracerExtractor.headers.size(), 3)
        assertEquals(tracerExtractor.headers.get(CONTENT_TYPE).size(), 1)
        assertEquals(tracerExtractor.headers.get(ACCEPT).size(), 2)
        assertEquals(tracerExtractor.headers.get(TRANS_ID_HEADER).size(), 1)
    }

    @Test
    void testIterateWithHeaders() {
        TracerExtractor tracerExtractor = new TracerExtractor(
            requestWrapper)
        Iterator<Map.Entry<String,String>> iterable = tracerExtractor.iterator()

        assertEquals("Validate 3 values in iterable",
            iterable.size(), 3)
    }

    @Test
    void testIterateWithMultiValueHeaders() {
        requestWrapper.addHeader(ACCEPT, "application/xml")

        TracerExtractor tracerExtractor = new TracerExtractor(
            requestWrapper)
        Iterator<Map.Entry<String,String>> iterable = tracerExtractor.iterator()

        assertEquals("Validate 4 values in iterable",
            iterable.size(), 4)

        while (iterable.hasNext())
            iterable.next()

        assertEquals("Validate able to iterate through iterable",
            iterable.size(), 0)
    }

    @Test(expected = UnsupportedOperationException.class)
    void testPutThrowsInvalidOperation() {
        TracerExtractor tracerExtractor = new TracerExtractor(
            requestWrapper)
        tracerExtractor.put("key", "value")
    }
}
