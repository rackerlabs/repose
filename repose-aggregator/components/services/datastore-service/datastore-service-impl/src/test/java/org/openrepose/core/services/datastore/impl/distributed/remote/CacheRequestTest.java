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
package org.openrepose.core.services.datastore.impl.distributed.remote;

import org.junit.Test;
import org.openrepose.commons.utils.http.ExtendedHttpHeader;
import org.openrepose.commons.utils.io.stream.ServletInputStreamWrapper;
import org.openrepose.core.services.datastore.distributed.RemoteBehavior;
import org.openrepose.core.services.datastore.impl.distributed.CacheRequest;
import org.openrepose.core.services.datastore.impl.distributed.DatastoreHeader;
import org.openrepose.core.services.datastore.impl.distributed.MalformedCacheRequestException;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CacheRequestTest {

    private HttpServletRequest mockRequest = mock(HttpServletRequest.class);

    private static final String RESOURCE = "3ae04e3f-164e-ab96-04d2-51aa51104daa";

    private HttpServletRequest mockRequestWithMethod(String method, String remoteHost) {
        return mockRequestWithMethod(RESOURCE, method, remoteHost);
    }

    private HttpServletRequest mockRequestWithMethod(String cacheKey, String method, String remoteHost) {
        final HttpServletRequest mockedRequest = mock(HttpServletRequest.class);
        when(mockedRequest.getRequestURI()).thenReturn(CacheRequest.CACHE_URI_PATH + cacheKey);
        when(mockedRequest.getMethod()).thenReturn(method);
        when(mockedRequest.getLocalAddr()).thenReturn("localhost");
        when(mockedRequest.getLocalPort()).thenReturn(2101);
        when(mockedRequest.getRemoteHost()).thenReturn(remoteHost);
        when(mockedRequest.getHeader(DatastoreHeader.REMOTE_BEHAVIOR)).thenReturn("ALLOW_FORWARDING");

        return mockedRequest;
    }

    @Test
    public void shouldPassIfRequestPathStartsWithCacheURI() {
        when(mockRequest.getRequestURI()).thenReturn(CacheRequest.CACHE_URI_PATH + "/foobar");
        assertTrue(CacheRequest.isCacheRequestValid(mockRequest));
    }

    @Test
    public void shouldNotMatchIfRequestPathDoesntStartWithCacheURI() {
        when(mockRequest.getRequestURI()).thenReturn("/stuff/" + CacheRequest.CACHE_URI_PATH + "/foobar");
        assertFalse(CacheRequest.isCacheRequestValid(mockRequest));
    }

    @Test
    public void shouldMarshallGetRequests() throws UnknownHostException {
        final InetSocketAddress addr = new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000);
        final String urlFor = CacheRequest.urlFor(addr, RESOURCE, false);

        assertEquals("Cache request must generate valid cache URLs", "http://127.0.0.1:1000" + CacheRequest.CACHE_URI_PATH + RESOURCE, urlFor);
    }

    @Test
    public void shouldBuildHttpRequestWithoutKey() throws UnknownHostException {
        final InetSocketAddress addr = new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000);
        final String urlFor = CacheRequest.urlFor(addr, false);

        assertEquals("Cache request must generate valid cache URLs", "http://127.0.0.1:1000" + CacheRequest.CACHE_URI_PATH, urlFor);
    }

    @Test
    public void shouldBuildHttpRequestWithKey() throws UnknownHostException {
        final InetSocketAddress addr = new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000);
        final String urlFor = CacheRequest.urlFor(addr, RESOURCE, false);

        assertEquals("Cache request must generate valid cache URLs", "http://127.0.0.1:1000" + CacheRequest.CACHE_URI_PATH + RESOURCE, urlFor);
    }

    @Test
    public void shouldBuildHttpsRequestWithoutKey() throws UnknownHostException {
        final InetSocketAddress addr = new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000);
        final String urlFor = CacheRequest.urlFor(addr, true);

        assertEquals("Cache request must generate valid cache URLs", "https://127.0.0.1:1000" + CacheRequest.CACHE_URI_PATH, urlFor);
    }

    @Test
    public void shouldBuildHttpsRequestWithKey() throws UnknownHostException {
        final InetSocketAddress addr = new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000);
        final String urlFor = CacheRequest.urlFor(addr, RESOURCE, true);

        assertEquals("Cache request must generate valid cache URLs", "https://127.0.0.1:1000" + CacheRequest.CACHE_URI_PATH + RESOURCE, urlFor);
    }

    @Test
    public void shouldUseDefaultRemoteBehaviorDirectiveWhenOneIsNotSetInTheRequest() {
        final HttpServletRequest request = mockRequestWithMethod("GET", "localhost");
        when(request.getHeader(DatastoreHeader.REMOTE_BEHAVIOR)).thenReturn(null);

        final CacheRequest cacheRequest = CacheRequest.marshallCacheRequest(request);

        assertEquals("Cache request must understand remote behavior directives", RemoteBehavior.ALLOW_FORWARDING, cacheRequest.getRequestedRemoteBehavior());
    }

    @Test
    public void shouldIgnoreCaseForRemoteBehaviorDirective() {
        final HttpServletRequest request = mockRequestWithMethod("GET", "localhost");
        when(request.getHeader(DatastoreHeader.REMOTE_BEHAVIOR)).thenReturn("diSallOw_forwaRding");

        final CacheRequest cacheRequest = CacheRequest.marshallCacheRequest(request);

        assertEquals("Cache request must understand remote behavior directives", RemoteBehavior.DISALLOW_FORWARDING, cacheRequest.getRequestedRemoteBehavior());
    }

    @Test(expected = MalformedCacheRequestException.class)
    public void shouldRejectBadRemoteBehaviorDirectives() {
        final HttpServletRequest request = mockRequestWithMethod("GET", "localhost");
        when(request.getHeader(DatastoreHeader.REMOTE_BEHAVIOR)).thenReturn("FAIL");

        CacheRequest.marshallCacheRequest(request);
    }

    @Test(expected = MalformedCacheRequestException.class)
    public void shouldRejectBlankCacheKeyUri() {
        final HttpServletRequest request = mockRequestWithMethod("", "GET", "localhost");
        CacheRequest.marshallCacheRequest(request);
    }

    @Test(expected = MalformedCacheRequestException.class)
    public void shouldRejectBadCacheKeyUri() {
        final HttpServletRequest request = mockRequestWithMethod("fail", "GET", "localhost");
        CacheRequest.marshallCacheRequest(request);
    }

    @Test
    public void whenProcessingCacheGetRequestsShouldMarshallRequest() {
        final HttpServletRequest request = mockRequestWithMethod("GET", "localhost");
        final CacheRequest cacheRequest = CacheRequest.marshallCacheRequest(request);

        assertEquals("Cache request must correctly identify the cache key", RESOURCE, cacheRequest.getCacheKey());
        assertEquals("Cache request must understand remote behavior directives", RemoteBehavior.ALLOW_FORWARDING, cacheRequest.getRequestedRemoteBehavior());
    }

    @Test(expected = MalformedCacheRequestException.class)
    public void shouldRejectBadTTL() throws IOException {
        final HttpServletRequest request = mockRequestWithMethod("PUT", "localhost");
        when(request.getHeader(ExtendedHttpHeader.X_TTL)).thenReturn("nan");

        CacheRequest.marshallCacheRequestWithPayload(request);
    }

    @Test(expected = MalformedCacheRequestException.class)
    public void shouldRejectNegativeTTL() throws IOException {
        final HttpServletRequest request = mockRequestWithMethod("PUT", "localhost");
        when(request.getHeader(ExtendedHttpHeader.X_TTL)).thenReturn("-1");

        CacheRequest.marshallCacheRequestWithPayload(request);
    }

    @Test(expected = MalformedCacheRequestException.class)
    public void shouldRejectCacheObjectsThatAreTooLarge() throws IOException {
        final HttpServletRequest request = mockRequestWithMethod("PUT", "localhost");
        when(request.getInputStream()).thenReturn(new ServletInputStreamWrapper(new ByteArrayInputStream(new byte[CacheRequest.TWO_MEGABYTES_IN_BYTES + 10])));

        CacheRequest.marshallCacheRequestWithPayload(request);
    }

    @Test
    public void shouldUseDefaultTTLWhenNotSpecified() throws IOException {
        final HttpServletRequest request = mockRequestWithMethod("PUT", "localhost");
        when(request.getInputStream()).thenReturn(new ServletInputStreamWrapper(new ByteArrayInputStream(new byte[]{1})));

        final CacheRequest cacheRequest = CacheRequest.marshallCacheRequestWithPayload(request);

        assertEquals("Cache request must correctly parse desired cache object TTL", CacheRequest.DEFAULT_TTL_IN_SECONDS, cacheRequest.getTtlInSeconds());
    }

    @Test
    public void whenProcessingCachePutRequestsShouldMarshallRequest() throws IOException {
        final HttpServletRequest request = mockRequestWithMethod("PUT", "localhost");
        when(request.getHeader(ExtendedHttpHeader.X_TTL)).thenReturn("5");
        when(request.getInputStream()).thenReturn(new ServletInputStreamWrapper(new ByteArrayInputStream(new byte[]{1})));

        final CacheRequest cacheRequest = CacheRequest.marshallCacheRequestWithPayload(request);

        assertEquals("Cache request must correctly identify the cache key", RESOURCE, cacheRequest.getCacheKey());
        assertEquals("Cache request must correctly parse desired cache object TTL", 5, cacheRequest.getTtlInSeconds());
        assertTrue("Cache request must correctly identify that is has content", cacheRequest.hasPayload());
        assertEquals("Cache request must correctly read the request content body", 1, cacheRequest.getPayload().length);
        assertEquals("Cache request must understand remote behavior directives", RemoteBehavior.ALLOW_FORWARDING, cacheRequest.getRequestedRemoteBehavior());
    }
}
