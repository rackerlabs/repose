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
package org.openrepose.filters.versioning.domain;

import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.filters.versioning.config.ServiceVersionMapping;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VersionedRequestTest {

    List<MediaType> mediaRangeList;
    ServiceVersionMapping mapping;

    @Before
    public void standUp() {
        mediaRangeList = new LinkedList<>();
        mediaRangeList.add(new MediaType("", MimeType.UNKNOWN, -1));

        mapping = new ServiceVersionMapping();
        mapping.setId("v1.0");
    }

    @Test
    public void shouldIdentifyVersion() {
        final HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);
        when(request.getRequestURI()).thenReturn("/v1.0/resource");

        final VersionedRequest versionedRequest = new VersionedRequest(request, mapping);

        assertTrue(versionedRequest.requestBelongsToVersionMapping());
    }

    @Test
    public void shouldIdentifyVersionWithTrailingSlash() {
        final HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);
        when(request.getRequestURI()).thenReturn("/v1.0/resource/");

        final VersionedRequest versionedRequest = new VersionedRequest(request, mapping);

        assertTrue(versionedRequest.requestBelongsToVersionMapping());
    }

    @Test
    public void shouldNotMatchPartialVersionMatches() {
        final HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);
        when(request.getRequestURI()).thenReturn("/v1.01/resource/");

        final VersionedRequest versionedRequest = new VersionedRequest(request, mapping);

        assertFalse(versionedRequest.requestBelongsToVersionMapping());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptUriWithoutRoot() {
        final HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);
        when(request.getRequestURI()).thenReturn("a/requested/resource");

        new VersionedRequest(request, mapping).asInternalURI();
    }

    @Test
    public void shouldHandleFuzzedRequests() {
        final HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);
        when(request.getRequestURI()).thenReturn("/v1.0a/requested/resource");

        final String expected = "/v1.0a/requested/resource";

        assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(request, mapping).asInternalURI());
    }

    @Test
    public void shouldHandleNonVersionedRequests() {
        final HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);
        when(request.getRequestURI()).thenReturn("/a/requested/resource");

        final String expected = "/a/requested/resource";

        assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(request, mapping).asInternalURI());
    }

    @Test
    public void shouldHandleVersionedRequestsWithContextRoot() {
        final HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);
        when(request.getRequestURI()).thenReturn("/context/v1.0/a/requested/resource");

        final String expected = "/context/a/requested/resource";

        assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(request, mapping).asInternalURI());
    }

    @Test
    public void shouldNotRewriteVersionedUri() {
        final String expected = "/_v1.0/a/requested/resource";
        final HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);
        when(request.getRequestURI()).thenReturn(expected);

        final VersionedRequest versionedRequest = new VersionedRequest(request, mapping);

        assertEquals("Formatting internal URI must match " + expected, expected, versionedRequest.asInternalURI());
    }

    @Test
    public void shouldHandleVersionedRequests() {
        final HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);
        when(request.getRequestURI()).thenReturn("/v1.0/a/requested/resource");

        final String expected = "/a/requested/resource";

        assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(request, mapping).asInternalURI());
    }

    @Test
    public void shouldBuildAccurateURLs() {
        final HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);
        when(request.getRequestURI()).thenReturn("/a/requested/resource");
        when(request.getScheme()).thenReturn("http");
        when(request.getHeader(HttpHeaders.HOST)).thenReturn("localhost");

        final String expected = "http://localhost/a/requested/resource";

        assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(request, mapping).asInternalURL());
    }

    @Test
    public void shouldHandleExternalRequestsWithContextRoot() {
        final HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);
        when(request.getRequestURI()).thenReturn("/v1.0/a/requested/resource");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/v1.0/a/requested/resource"));

        final String expected = "http://localhost/v1.0/a/requested/resource";

        assertEquals("Formatting internal URI must match " + expected, expected, new VersionedRequest(request, mapping).asExternalURL());
    }

    @Test
    public void shouldMatch() {
        final HttpServletRequestWrapper versionOne = mock(HttpServletRequestWrapper.class);
        final HttpServletRequestWrapper versionOneWithResource = mock(HttpServletRequestWrapper.class);
        final HttpServletRequestWrapper versionTwo = mock(HttpServletRequestWrapper.class);
        when(versionOne.getRequestURI()).thenReturn("/v1.0");
        when(versionOneWithResource.getRequestURI()).thenReturn("/v1.0/some/resource");
        when(versionTwo.getRequestURI()).thenReturn("/v2.0/some/resource");

        assertTrue(new VersionedRequest(versionOne, mapping).requestBelongsToVersionMapping());
        assertTrue(new VersionedRequest(versionOneWithResource, mapping).requestBelongsToVersionMapping());
        assertFalse(new VersionedRequest(versionTwo, mapping).requestBelongsToVersionMapping());
    }

    @Test
    public void shouldIdentifyOwningVersions() {
        final HttpServletRequestWrapper versionOne = mock(HttpServletRequestWrapper.class);
        final HttpServletRequestWrapper versionTwo = mock(HttpServletRequestWrapper.class);
        when(versionOne.getRequestURI()).thenReturn("/v1.0/some/resource");
        when(versionTwo.getRequestURI()).thenReturn("/v2.0/some/resource");

        assertTrue(new VersionedRequest(versionOne, mapping).requestBelongsToVersionMapping());
        assertFalse(new VersionedRequest(versionTwo, mapping).requestBelongsToVersionMapping());
    }
}
