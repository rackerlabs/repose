package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.util.encoding.UUIDEncodingProvider;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.io.stream.ServletInputStreamWrapper;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.service.datastore.DatastoreAccessControl;
import com.rackspace.papi.components.datastore.impl.distributed.CacheRequest;
import com.rackspace.papi.components.datastore.impl.distributed.DatastoreHeader;
import com.rackspace.papi.components.datastore.distributed.RemoteBehavior;
import com.rackspace.papi.components.datastore.impl.distributed.HashRingDatastore;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class DatastoreFilterLogicHandlerTest {

   @Ignore
   public static class TestParent {

      protected static final String RESOURCE = "3ae04e3f-164e-ab96-04d2-51aa51104daa";
      protected ReadableHttpServletResponse mockResponse;
      protected DatastoreFilterLogicHandler logicHandler;
      protected DatastoreAccessControl dac;
      protected HashRingDatastore hashedDatastore;

      @Before
      public void beforeAny() throws Exception {
         hashedDatastore = mock(HashRingDatastore.class);
         when(hashedDatastore.get(anyString(), any(byte[].class), any(RemoteBehavior.class))).thenReturn(new StoredElementImpl("", null));

         mockResponse = mock(ReadableHttpServletResponse.class);

         final List<InetAddress> allowedAddresses = new LinkedList<InetAddress>();
         allowedAddresses.add(InetAddress.getByName("10.1.1.1"));

         dac = new DatastoreAccessControl(allowedAddresses, false);
         logicHandler = new DatastoreFilterLogicHandler(UUIDEncodingProvider.getInstance(), hashedDatastore, dac);
      }

      public HttpServletRequest mockRequestWithMethod(String method, String remoteHost) {
         final HttpServletRequest mockedRequest = mock(HttpServletRequest.class);
         when(mockedRequest.getRequestURI()).thenReturn(CacheRequest.CACHE_URI_PATH + RESOURCE);
         when(mockedRequest.getMethod()).thenReturn(method);
         when(mockedRequest.getLocalAddr()).thenReturn("localhost");
         when(mockedRequest.getLocalPort()).thenReturn(2101);
         when(mockedRequest.getRemoteHost()).thenReturn(remoteHost);
         when(mockedRequest.getHeader(DatastoreHeader.HOST_KEY.toString())).thenReturn("temp");

         return mockedRequest;
      }
   }

   public static class WhenHandlingRequests extends TestParent {

      @Test
      public void shouldGetCacheObjects() {
         final HttpServletRequest mockGetRequest = mockRequestWithMethod("GET", "10.1.1.1");

         logicHandler.handleRequest(mockGetRequest, mockResponse);

         verify(hashedDatastore, times(1)).get(eq(RESOURCE), any(byte[].class), eq(RemoteBehavior.ALLOW_FORWARDING));
      }

      @Test
      public void shouldPutCacheObjects() throws Exception {
         final ServletInputStream inputStream = new ServletInputStreamWrapper(
                 new ByteArrayInputStream(new byte[]{'t', 'e', 's', 't'}));

         final HttpServletRequest mockPutRequest = mockRequestWithMethod("PUT", "10.1.1.1");
         when(mockPutRequest.getInputStream()).thenReturn(inputStream);

         logicHandler.handleRequest(mockPutRequest, mockResponse);

         verify(hashedDatastore, times(1)).put(eq(RESOURCE), any(byte[].class), any(byte[].class), eq(60), eq(TimeUnit.SECONDS), eq(RemoteBehavior.ALLOW_FORWARDING));
      }

      @Test
      public void shouldDeleteCacheObjects() {
         final HttpServletRequest mockGetRequest = mockRequestWithMethod("DELETE", "10.1.1.1");

         logicHandler.handleRequest(mockGetRequest, mockResponse);

         verify(hashedDatastore, times(1)).remove(eq(RESOURCE), any(byte[].class), eq(RemoteBehavior.ALLOW_FORWARDING));
      }

      @Test
      public void shouldIgnoreNonCacheRequests() {
         final HttpServletRequest mockGetRequest = mockRequestWithMethod("GET", "10.1.1.1");
         when(mockGetRequest.getRequestURI()).thenReturn("/the/magic/path");
         
         final FilterDirector director = logicHandler.handleRequest(mockGetRequest, mockResponse);

         assertEquals("Datastore must pass non-cache requests", FilterAction.PASS, director.getFilterAction());
      }

      @Test
      public void shouldRejectUnknownMethods() {
         final HttpServletRequest mockGetRequest = mockRequestWithMethod("TRACE", "10.1.1.1");

         final FilterDirector director = logicHandler.handleRequest(mockGetRequest, mockResponse);

         assertEquals("Datastore must fail cache requests is can not handle", FilterAction.RETURN, director.getFilterAction());
         assertEquals("Datastore must fail cache requests is can not handle", HttpStatusCode.NOT_IMPLEMENTED, director.getResponseStatus());
      }

      @Test
      public void shouldRejectBadCacheRequests() {
         final HttpServletRequest mockGetRequest = mockRequestWithMethod("GET", "10.1.1.1");
         when(mockGetRequest.getRequestURI()).thenReturn(CacheRequest.CACHE_URI_PATH + "fail");

         final FilterDirector director = logicHandler.handleRequest(mockGetRequest, mockResponse);

         assertEquals("Datastore must fail cache requests is can not handle", FilterAction.RETURN, director.getFilterAction());
         assertEquals("Datastore must fail cache requests is can not handle", HttpStatusCode.BAD_REQUEST, director.getResponseStatus());
      }
   }

   @Ignore
   public static class WhenHandlingBadCacheRequests extends TestParent {

      @Test
      public void shouldReturnCorrectHTTPStatusCode() {
         final HttpServletRequest mockGetRequest = mockRequestWithMethod("GET", "10.1.1.1");
         when(mockGetRequest.getHeader(eq(DatastoreHeader.HOST_KEY.toString()))).thenReturn(null);

         final FilterDirector director = logicHandler.handleRequest(mockGetRequest, mockResponse);

         assertEquals("Datastore must pass requests is can not handle", FilterAction.PASS, director.getFilterAction());
      }
   }

   public static class WhenValidatingAddressAccessControl extends TestParent {

      @Test
      public void shouldRejectHostsWithoutAccess() {
         final HttpServletRequest mockGetRequest = mockRequestWithMethod("GET", "10.1.1.2");
         final FilterDirector actual = logicHandler.handleRequest(mockGetRequest, mockResponse);

         assertEquals("Unauthorized requests must be returned", FilterAction.RETURN, actual.getFilterAction());
         assertEquals("Unauthorized requests should be marked with status code 403 (forbidden)", HttpStatusCode.FORBIDDEN, actual.getResponseStatus());
      }

      @Test
      public void shouldAllowHostsAddressesListedInTheHostACL() {
         final HttpServletRequest mockGetRequest = mockRequestWithMethod("GET", "10.1.1.1");
         final FilterDirector actual = logicHandler.handleRequest(mockGetRequest, mockResponse);

         assertEquals("Authorized requests must be returned", FilterAction.RETURN, actual.getFilterAction());
         assertEquals("Request should return not-found for allowed requests that have nothing stored at the requested resource", HttpStatusCode.NOT_FOUND, actual.getResponseStatus());
      }
   }

   public static class WhenDictatingRemoteBehavior extends TestParent {

      @Test
      public void shouldSetRemoteBehaviorForRequestedOperation() {
         final HttpServletRequest mockGetRequest = mockRequestWithMethod("GET", "10.1.1.1");
         when(mockGetRequest.getHeader(DatastoreHeader.REMOTE_BEHAVIOR.toString())).thenReturn("diSallOw_forwaRding");

         logicHandler.handleRequest(mockGetRequest, mockResponse);

         verify(hashedDatastore, times(1)).get(eq(RESOURCE), any(byte[].class), eq(RemoteBehavior.DISALLOW_FORWARDING));
      }
   }
}
