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
package org.openrepose.filters.ratelimiting

import org.openrepose.commons.utils.http.PowerApiHeader
import org.openrepose.commons.utils.http.media.MimeType
import org.openrepose.core.services.ratelimit.RateLimitingService
import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.MediaType

import static org.mockito.Mockito.*

public class RateLimitingServiceHelperTest extends Specification {
    private static final String MOST_QUALIFIED_USER = "the best user of them all"
    private static final String MOST_QUALIFIED_GROUP = "the best group of them all"

    @Shared
    private RateLimitingServiceHelper helper = new RateLimitingServiceHelper(null, null, null)

    @Shared
    private HttpServletRequest mockedRequest

    def setupSpec() {
        mockedRequest = mock(HttpServletRequest.class)
    }

    def "when getting preferred MediaType, should get Java MediaType from Repose MimeType"() {
        given:
        MimeType reposeMimeType = MimeType.APPLICATION_XML

        when:
        MediaType javaMediaType = helper.getJavaMediaType(reposeMimeType)

        then:
        javaMediaType.toString() == MediaType.APPLICATION_XML
    }

    def "when getting preferred user, should return most qualified user header"() {
        given:
        List<String> headerValues = new LinkedList<String>()
        headerValues.add(MOST_QUALIFIED_USER + ";q=1.0")
        headerValues.add("that other user;q=0.5")
        headerValues.add("127.0.0.1;q=0.1")

        when(mockedRequest.getHeaders(PowerApiHeader.USER)).thenReturn(Collections.enumeration(headerValues))

        when:
        String user = helper.getPreferredUser(mockedRequest)

        then:
        user == MOST_QUALIFIED_USER
    }

    def "when getting preferred user, should return first user in list for users without quality factors present"() {
        given:
        when(mockedRequest.getHeaderNames())
                .thenReturn(Collections.enumeration(Collections.singletonList(PowerApiHeader.USER)))

        List<String> headerValues = new LinkedList<String>()
        headerValues.add(MOST_QUALIFIED_USER)
        headerValues.add("that other user")
        headerValues.add("127.0.0.1")

        when(mockedRequest.getHeaders(PowerApiHeader.USER)).thenReturn(Collections.enumeration(headerValues))

        when:
        String user = helper.getPreferredUser(mockedRequest)

        then:
        user == MOST_QUALIFIED_USER
    }

    def "when getting preferred group, should return most qualified groups"() {
        given:
        when(mockedRequest.getHeaderNames())
                .thenReturn(Collections.enumeration(Collections.singletonList(PowerApiHeader.GROUPS)))

        List<String> headerValues = new LinkedList<String>()
        headerValues.add("group-4;q=0.1")
        headerValues.add("group-2;q=0.1")
        headerValues.add("group-1;q=0.1")
        headerValues.add("group-3;q=0.002")

        when(mockedRequest.getHeaders(PowerApiHeader.GROUPS)).thenReturn(Collections.enumeration(headerValues))

        List<String> expected = new LinkedList<String>()
        expected.add("group-4")
        expected.add("group-2")
        expected.add("group-1")

        when:
        List<String> groups = helper.getPreferredGroups(mockedRequest)

        then:
        groups == expected
    }

    def "when getting preferred group, should handle multiple values on a single line"() {
        given:
        when(mockedRequest.getHeaderNames())
                .thenReturn(Collections.enumeration(Collections.singletonList(PowerApiHeader.GROUPS)))

        List<String> headerValues = new LinkedList<String>()
        headerValues.add("group-1;q=0.1,group-2;q=0.1")
        headerValues.add("group-4;q=0.1,group-5;q=0.1")
        headerValues.add("group-3;q=0.002")

        when(mockedRequest.getHeaders(PowerApiHeader.GROUPS)).thenReturn(Collections.enumeration(headerValues))

        List<String> expected = new LinkedList<String>()
        expected.add("group-1")
        expected.add("group-2")
        expected.add("group-4")
        expected.add("group-5")

        when:
        List<String> groups = helper.getPreferredGroups(mockedRequest)

        then:
        groups == expected
    }

    def "when getting preferred group, should return empty group list when no groups header is present"() {
        given:
        when(mockedRequest.getHeaders(PowerApiHeader.GROUPS)).thenReturn(Collections.emptyEnumeration())

        when:
        List<String> groups = helper.getPreferredGroups(mockedRequest)

        then:
        groups.size() == 0
    }

    def "when getting preferred group, should return all groups when quality factor is not present"() {
        given:
        when(mockedRequest.getHeaderNames())
                .thenReturn(Collections.enumeration(Collections.singletonList(PowerApiHeader.GROUPS)))

        List<String> headerValues = new LinkedList<String>()
        headerValues.add(MOST_QUALIFIED_GROUP)
        headerValues.add("group-4")
        headerValues.add("group-2")
        headerValues.add("group-1")
        headerValues.add("group-3")

        when(mockedRequest.getHeaders(PowerApiHeader.GROUPS)).thenReturn(Collections.enumeration(headerValues))

        List<String> expected = new LinkedList<String>()
        expected.add(MOST_QUALIFIED_GROUP)
        expected.add("group-4")
        expected.add("group-2")
        expected.add("group-1")
        expected.add("group-3")

        when:
        List<String> groups = helper.getPreferredGroups(mockedRequest)

        then:
        groups == expected
    }

    @Unroll
    def "when getting URI, #expectedBehavior"() {
        given:
        def mockRlService = mock(RateLimitingService.class)
        def rateLimitingServiceHelper = new RateLimitingServiceHelper(mockRlService, null, null)
        when(mockedRequest.getHeaders(PowerApiHeader.GROUPS)).thenReturn(Collections.emptyEnumeration())

        MockHttpServletRequest request = new MockHttpServletRequest()
        request.setRequestURI(requestURI)

        when:
        rateLimitingServiceHelper.trackLimits(request, 1000)

        then:
        verify(mockRlService).trackLimits(any(String.class), any(List.class), eq(decodedURI), any(Map.class), any(String.class), anyInt())

        where:
        requestURI               | decodedURI       | expectedBehavior
        "/foo/bar/baz"           | "/foo/bar/baz"   | "should not alter unencoded URI"
        "/foo/%6A%61%72/baz"     | "/foo/jar/baz"   | "should decode uppercase encoded URI"
        "/foo/%6a%61%72/baz"     | "/foo/jar/baz"   | "should decode lowercase encoded URI"
        "/foo/%62%61%72/baz%20+" | "/foo/bar/baz +" | "should decode encoded URI with plus sign and space"
        "/foo/ba%2Fr/baz"        | "/foo/ba/r/baz"  | "should decode encoded URI with encoded forward slash"
    }
}
