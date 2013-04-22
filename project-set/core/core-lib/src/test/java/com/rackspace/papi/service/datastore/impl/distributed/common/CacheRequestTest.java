/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.service.datastore.impl.distributed.common;

import com.rackspace.papi.commons.util.http.ExtendedHttpHeader;
import com.rackspace.papi.commons.util.io.stream.ServletInputStreamWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class CacheRequestTest {
   
   public static final String RESOURCE = "3ae04e3f-164e-ab96-04d2-51aa51104daa";

   @Ignore
   public static class TestParent {

      @Before
      public void standUp() {
      }

      public HttpServletRequest mockRequestWithMethod(String method, String remoteHost) {
         return mockRequestWithMethod(RESOURCE, method, remoteHost);
      }

      public HttpServletRequest mockRequestWithMethod(String cacheKey, String method, String remoteHost) {
         final HttpServletRequest mockedRequest = mock(HttpServletRequest.class);
         when(mockedRequest.getRequestURI()).thenReturn(CacheRequest.CACHE_URI_PATH + cacheKey);
         when(mockedRequest.getMethod()).thenReturn(method);
         when(mockedRequest.getLocalAddr()).thenReturn("localhost");
         when(mockedRequest.getLocalPort()).thenReturn(2101);
         when(mockedRequest.getRemoteHost()).thenReturn(remoteHost);
         when(mockedRequest.getHeader(DatastoreHeader.HOST_KEY.toString())).thenReturn("temp");
         when(mockedRequest.getHeader(DatastoreHeader.REMOTE_BEHAVIOR.toString())).thenReturn("ALLOW_FORWARDING");

         return mockedRequest;
      }
   }

   public static class WhenBuildingURLsForCacheRequests extends TestParent {

      @Test
      public void shouldMarshallGetRequests() throws UnknownHostException {
         final InetSocketAddress addr = new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 1000);
         final String urlFor = CacheRequest.urlFor(addr, RESOURCE);

         assertEquals("Cache request must generate valid cache URLs", "http://127.0.0.1:1000" + CacheRequest.CACHE_URI_PATH + RESOURCE, urlFor);
      }
   }

   public static class WhenReadingHostKey extends TestParent {

      @Test(expected = MalformedCacheRequestException.class)
      public void shouldRejectRequestsThatHaveNoHostKey() {
         final HttpServletRequest request = mockRequestWithMethod("GET", "localhost");
         when(request.getHeader(DatastoreHeader.HOST_KEY.toString())).thenReturn(null);

         CacheRequest.marshallCacheRequest(request);
      }
   }

   public static class WhenReadingRemoteBehaviorDirectives extends TestParent {

      @Test
      public void shouldUseDefaultRemoteBehaviorDirectiveWhenOneIsNotSetInTheRequest() {
         final HttpServletRequest request = mockRequestWithMethod("GET", "localhost");
         when(request.getHeader(DatastoreHeader.REMOTE_BEHAVIOR.toString())).thenReturn(null);

         final CacheRequest cacheRequest = CacheRequest.marshallCacheRequest(request);

         assertEquals("Cache request must understand remote behavior directives", RemoteBehavior.ALLOW_FORWARDING, cacheRequest.getRequestedRemoteBehavior());
      }

      @Test
      public void shouldIgnoreCaseForRemoteBehaviorDirective() {
         final HttpServletRequest request = mockRequestWithMethod("GET", "localhost");
         when(request.getHeader(DatastoreHeader.REMOTE_BEHAVIOR.toString())).thenReturn("diSallOw_forwaRding");

         final CacheRequest cacheRequest = CacheRequest.marshallCacheRequest(request);

         assertEquals("Cache request must understand remote behavior directives", RemoteBehavior.DISALLOW_FORWARDING, cacheRequest.getRequestedRemoteBehavior());
      }

      @Test(expected = MalformedCacheRequestException.class)
      public void shouldRejectBadRemoteBehaviorDirectives() {
         final HttpServletRequest request = mockRequestWithMethod("GET", "localhost");
         when(request.getHeader(DatastoreHeader.REMOTE_BEHAVIOR.toString())).thenReturn("FAIL");

         CacheRequest.marshallCacheRequest(request);
      }
   }

   public static class WhenProcessingCacheGetRequests extends TestParent {

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
      public void shouldMarshallRequest() {
         final HttpServletRequest request = mockRequestWithMethod("GET", "localhost");
         final CacheRequest cacheRequest = CacheRequest.marshallCacheRequest(request);

         assertEquals("Cache request must correctly identify the cache key", RESOURCE, cacheRequest.getCacheKey());
         assertEquals("Cache request must understand remote behavior directives", RemoteBehavior.ALLOW_FORWARDING, cacheRequest.getRequestedRemoteBehavior());
      }
   }

   public static class WhenProcessingCachePutRequests extends TestParent {

      @Test(expected = MalformedCacheRequestException.class)
      public void shouldRejectBadTTL() throws IOException {
         final HttpServletRequest request = mockRequestWithMethod("PUT", "localhost");
         when(request.getHeader(ExtendedHttpHeader.X_TTL.toString())).thenReturn("nan");

         CacheRequest.marshallCachePutRequest(request);
      }

      @Test(expected = MalformedCacheRequestException.class)
      public void shouldRejectNegativeTTL() throws IOException {
         final HttpServletRequest request = mockRequestWithMethod("PUT", "localhost");
         when(request.getHeader(ExtendedHttpHeader.X_TTL.toString())).thenReturn("-1");

         CacheRequest.marshallCachePutRequest(request);
      }

      @Test(expected = MalformedCacheRequestException.class)
      public void shouldRejectCacheObjectsThatAreTooLarge() throws IOException {
         final HttpServletRequest request = mockRequestWithMethod("PUT", "localhost");
         when(request.getInputStream()).thenReturn(new ServletInputStreamWrapper(new ByteArrayInputStream(new byte[CacheRequest.TWO_MEGABYTES_IN_BYTES + 10])));

         CacheRequest.marshallCachePutRequest(request);
      }

      @Test
      public void shouldUseDefaultTTLWhenNotSpecified() throws IOException {
         final HttpServletRequest request = mockRequestWithMethod("PUT", "localhost");
         when(request.getInputStream()).thenReturn(new ServletInputStreamWrapper(new ByteArrayInputStream(new byte[]{1})));

         final CacheRequest cacheRequest = CacheRequest.marshallCachePutRequest(request);

         assertEquals("Cache request must correctly parse desired cache object TTL", CacheRequest.DEFAULT_TTL_IN_SECONDS, cacheRequest.getTtlInSeconds());
      }

      @Test
      public void shouldMarshallRequest() throws IOException {
         final HttpServletRequest request = mockRequestWithMethod("PUT", "localhost");
         when(request.getHeader(ExtendedHttpHeader.X_TTL.toString())).thenReturn("5");
         when(request.getInputStream()).thenReturn(new ServletInputStreamWrapper(new ByteArrayInputStream(new byte[]{1})));

         final CacheRequest cacheRequest = CacheRequest.marshallCachePutRequest(request);

         assertEquals("Cache request must correctly identify the cache key", RESOURCE, cacheRequest.getCacheKey());
         assertEquals("Cache request must correctly parse desired cache object TTL", 5, cacheRequest.getTtlInSeconds());
         assertTrue("Cache request must correctly identify that is has content", cacheRequest.hasPayload());
         assertEquals("Cache request must correctly read the request content body", 1, cacheRequest.getPayload().length);
         assertEquals("Cache request must understand remote behavior directives", RemoteBehavior.ALLOW_FORWARDING, cacheRequest.getRequestedRemoteBehavior());
      }
   }
}