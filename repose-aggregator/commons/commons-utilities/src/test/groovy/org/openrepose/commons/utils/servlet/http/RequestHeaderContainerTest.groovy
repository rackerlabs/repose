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
package org.openrepose.commons.utils.servlet.http

import org.junit.Before
import org.junit.Test
import org.openrepose.commons.utils.http.header.HeaderName

import javax.servlet.http.HttpServletRequest

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasItems
import static org.junit.Assert.assertThat
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class RequestHeaderContainerTest {
    RequestHeaderContainer container
    Enumeration<String> headerNames
    Enumeration<String> headerValues1
    Enumeration<String> headerValues2
    Enumeration<String> testEnum
    HttpServletRequest originalRequest
    String header1 = "val1.1"
    String header2 = "1.1 Proxy(blah , ), 1.1 Other(three,two,one)"

    @Before
    void setUp() {
        headerNames = createStringEnumeration("accept", "via", "Content-Type", "BLAH")
        headerValues1 = createStringEnumeration(header1, "val1.2")
        headerValues2 = createStringEnumeration(header2)
        testEnum = createStringEnumeration("test")

        originalRequest = mock(HttpServletRequest.class)

        when(originalRequest.getHeaderNames()).thenReturn(headerNames)
        when(originalRequest.getHeaders("accept")).thenReturn(headerValues1)
        when(originalRequest.getHeaders("via")).thenReturn(headerValues2)
        when(originalRequest.getHeaders("Content-Type")).thenReturn(testEnum)
        when(originalRequest.getHeaders("BLAH")).thenReturn(testEnum)
        when(originalRequest.getHeader("accept")).thenReturn(header1)
        when(originalRequest.getHeader("via")).thenReturn(header2)
        when(originalRequest.getHeader("Content-Type")).thenReturn("test")
        when(originalRequest.getHeader("BLAH")).thenReturn("test")

        container = new RequestHeaderContainer(originalRequest)
    }

    @Test
    void testGetHeaderNames() {
        assert container.getHeaderNames().size() == 4
        assert container.getHeaderNames().contains(HeaderName.wrap("accept"))
        assert container.getHeaderNames().contains(HeaderName.wrap("via"))
    }

    @Test
    void testGetHeaderValues() {
        assert container.getHeaderValues("accept").size() == 2
        assert container.getHeaderValues("via").size() == 1
    }

    @Test
    void testContainsHeader() {
        assert container.containsHeader("accept")
        assert container.containsHeader("via")
    }

    @Test
    void testGetContainerType() {
        assert container.getContainerType() == HeaderContainerType.REQUEST
    }

    @Test
    void "header names are not modified"() throws Exception {
        assertThat(container.getHeaderNames().size(), equalTo(4))
        assertThat(container.getHeaderNames(), hasItems(
                HeaderName.wrap("accept"),
                HeaderName.wrap("via"),
                HeaderName.wrap("BLAH"),
                HeaderName.wrap("Content-Type")))
    }

    static Enumeration<String> createStringEnumeration(String... names) {
        Vector<String> namesCollection = new Vector<String>(names.length)

        namesCollection.addAll(Arrays.asList(names))

        return namesCollection.elements()
    }
}
