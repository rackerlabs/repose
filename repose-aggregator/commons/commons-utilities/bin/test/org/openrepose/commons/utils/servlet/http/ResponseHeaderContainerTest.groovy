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

import javax.servlet.http.HttpServletResponse

import static junit.framework.Assert.assertFalse
import static junit.framework.Assert.assertTrue
import static org.hamcrest.Matchers.*
import static org.junit.Assert.assertThat
import static org.mockito.Matchers.argThat
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class ResponseHeaderContainerTest {
    public static final List<String> HEADER_NAME_LIST = ["via", "BLAH", "Content-Type"] as List<String>

    ResponseHeaderContainer responseHeaderContainer

    @Before
    void setup() {
        HttpServletResponse response = mock(HttpServletResponse.class)

        when(response.getHeaderNames()).thenReturn(HEADER_NAME_LIST)
        when(response.getHeaders(argThat(equalToIgnoringCase("via")))).thenReturn(["repose"] as List<String>)
        when(response.getHeaders(argThat(equalToIgnoringCase("BLAH")))).thenReturn(["blah"] as List<String>)
        when(response.getHeaders(argThat(equalToIgnoringCase("content-type")))).thenReturn(["text/plain"] as List<String>)
        when(response.getHeader(argThat(equalToIgnoringCase("via")))).thenReturn("repose")
        when(response.getHeader(argThat(equalToIgnoringCase("BLAH")))).thenReturn("blah")
        when(response.getHeader(argThat(equalToIgnoringCase("content-type")))).thenReturn("text/plain")

        responseHeaderContainer = new ResponseHeaderContainer(response)
    }

    @Test
    void "header names are not modified"() throws Exception {
        assertThat(responseHeaderContainer.getHeaderNames().size(), equalTo(3))
        assertThat(responseHeaderContainer.getHeaderNames(), hasItems(
                HeaderName.wrap("via"),
                HeaderName.wrap("BLAH"),
                HeaderName.wrap("Content-Type")))
    }

    @Test
    void "getHeaderValues returns a list with the default quality and a value of \"repose\""() throws Exception {
        assertThat(responseHeaderContainer.getHeaderValues("via").size(), equalTo(1))
        assertThat(responseHeaderContainer.getHeaderValues("via").get(0).getValue(), equalTo("repose"))
        assertThat(responseHeaderContainer.getHeaderValues("via").get(0).getQualityFactor(), equalTo(1.0.doubleValue()))
    }

    @Test
    void "containsHeader returns true if a header is present and false otherwise"() throws Exception {
        assertTrue(responseHeaderContainer.containsHeader("via"))
        assertFalse(responseHeaderContainer.containsHeader("not-a-header"))
    }

    @Test
    void "getContainerType returns the response container type"() throws Exception {
        assertThat(responseHeaderContainer.getContainerType(), equalTo(HeaderContainerType.RESPONSE))
    }
}
