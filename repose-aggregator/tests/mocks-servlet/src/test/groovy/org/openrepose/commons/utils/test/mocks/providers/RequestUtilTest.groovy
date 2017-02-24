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
package org.openrepose.commons.utils.test.mocks.providers

import org.junit.Test

import javax.servlet.http.HttpServletRequest

import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertThat
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class RequestUtilTest {

    @Test
    void testServletRequestToXml() {

        HttpServletRequest servletRequest = mock(HttpServletRequest.class)
        when(servletRequest.getMethod()).thenReturn("PATCH")
        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://test.openrepose.org/path/to/resource"))
        when(servletRequest.getRequestURI()).thenReturn("/path/to/resource")
        when(servletRequest.getInputStream()).thenReturn(null);
        when(servletRequest.getQueryString()).thenReturn("q=1&q=2&r=3&r=4")

        Enumeration<String> headerNames = createStringEnumeration("accept", "host", "x-pp-user")
        Enumeration<String> acceptValues = createStringEnumeration("application/xml")
        Enumeration<String> hostValues = createStringEnumeration("http://test.openrepose.org")
        Enumeration<String> userValues = createStringEnumeration("usertest1", "usertest2")

        when(servletRequest.getHeaderNames()).thenReturn(headerNames)
        when(servletRequest.getHeaders("accept")).thenReturn(acceptValues)
        when(servletRequest.getHeaders("host")).thenReturn(hostValues)
        when(servletRequest.getHeaders("x-pp-user")).thenReturn(userValues)

        Enumeration<String> queryNames = createStringEnumeration("q", "r")
        Map<String, String[]> queryValues = new HashMap<String, String[]>()
        String[] qValues = new String[2]
        String[] rValues = new String[2]

        qValues[0] = "1"
        qValues[1] = "2"

        rValues[0] = "3"
        rValues[1] = "4"

        queryValues.put("q", qValues)
        queryValues.put("r", rValues)
        when(servletRequest.getParameterNames()).thenReturn(queryNames)
        when(servletRequest.getParameterMap()).thenReturn(queryValues)

        String xml = RequestUtil.servletRequestToXml(servletRequest, "butts butts butts")
        def requestInfo = new XmlSlurper().parseText(xml)

        assertThat(requestInfo.method, equalTo("PATCH"))
        assertThat(requestInfo.path, equalTo("/path/to/resource"))
        assertThat(requestInfo.queryString, equalTo("q=1&q=2&r=3&r=4"))
    }

    static Enumeration<String> createStringEnumeration(String... names) {
        Vector<String> namesCollection = new Vector<String>(names.length)
        namesCollection.addAll(Arrays.asList(names))
        return namesCollection.elements()
    }

}
