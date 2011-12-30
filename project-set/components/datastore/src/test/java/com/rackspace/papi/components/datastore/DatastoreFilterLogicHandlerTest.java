package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.io.stream.ServletInputStreamWrapper;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.hash.HashedDatastore;
import com.rackspace.papi.service.datastore.impl.StoredElementImpl;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class DatastoreFilterLogicHandlerTest {

   @Ignore
   public static class TestParent {

      protected ReadableHttpServletResponse mockResponse;
      protected DatastoreFilterLogicHandler logicHandler;
      protected DatastoreAccessControl dac;
      protected HashedDatastore hashedDatastore;

      @Before
      public void beforeAny() throws Exception {
         hashedDatastore = mock(HashedDatastore.class);
         when(hashedDatastore.getByHash(anyString())).thenReturn(new StoredElementImpl("", null));

         mockResponse = mock(ReadableHttpServletResponse.class);

         final MutableClusterView mutableClusterView = mock(MutableClusterView.class);
         final List<InetAddress> allowedAddresses = new LinkedList<InetAddress>();
         allowedAddresses.add(InetAddress.getByName("10.1.1.1"));

         dac = new DatastoreAccessControl(allowedAddresses, false);
         logicHandler = new DatastoreFilterLogicHandler(mutableClusterView, "localhost", hashedDatastore, dac);
      }

      public HttpServletRequest mockRequestWithMethod(String method, String remoteHost) {
         final HttpServletRequest mockedRequest = mock(HttpServletRequest.class);
         when(mockedRequest.getRequestURI()).thenReturn(CacheRequest.CACHE_URI_PATH + "resource");
         when(mockedRequest.getMethod()).thenReturn(method);
         when(mockedRequest.getLocalAddr()).thenReturn("localhost");
         when(mockedRequest.getLocalPort()).thenReturn(2101);
         when(mockedRequest.getRemoteHost()).thenReturn(remoteHost);
         when(mockedRequest.getHeader(DatastoreRequestHeaders.DATASTORE_HOST_KEY)).thenReturn("temp");

         return mockedRequest;
      }
   }

   public static class WhenHandlingCacheRequests extends TestParent {

      @Test
      public void shouldGetCacheObjects() {
         final HttpServletRequest mockGetRequest = mockRequestWithMethod("GET", "10.1.1.1");

         logicHandler.handleRequest(mockGetRequest, mockResponse);

         verify(hashedDatastore, times(1)).getByHash(eq("resource"));
      }

      @Test
      public void shouldPutCacheObjects() throws Exception {
         final ServletInputStream inputStream = new ServletInputStreamWrapper(
                 new ByteArrayInputStream(new byte[]{'t', 'e', 's', 't'}));

         final HttpServletRequest mockPutRequest = mockRequestWithMethod("PUT", "10.1.1.1");
         when(mockPutRequest.getInputStream()).thenReturn(inputStream);

         logicHandler.handleRequest(mockPutRequest, mockResponse);

         verify(hashedDatastore, times(1)).putByHash(eq("resource"), any(byte[].class), eq(60), eq(TimeUnit.SECONDS));
      }

      @Test
      public void shouldDeleteCacheObjects() {
         final HttpServletRequest mockGetRequest = mockRequestWithMethod("DELETE", "10.1.1.1");

         logicHandler.handleRequest(mockGetRequest, mockResponse);

         verify(hashedDatastore, times(1)).removeByHash(eq("resource"));
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
}
