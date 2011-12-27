package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.hash.HashedDatastore;
import com.rackspace.papi.service.datastore.impl.StoredElementImpl;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
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
   public static class WhenHandlingCacheRequests {
   }

   public static class WhenValidatingAddressAccessControl {

      protected HttpServletRequest mockRequest;
      protected DatastoreFilterLogicHandler logicHandler;
      protected DatastoreAccessControl dac;
      
      @Before
      public void standUp() throws Exception {
         mockRequest = mock(HttpServletRequest.class);
         
         when(mockRequest.getRequestURI()).thenReturn(CacheRequest.CACHE_URI_PATH + "resource");
         when(mockRequest.getMethod()).thenReturn("GET");
         when(mockRequest.getLocalAddr()).thenReturn("localhost");
         when(mockRequest.getLocalPort()).thenReturn(2101);
         when(mockRequest.getHeader(DatastoreRequestHeaders.DATASTORE_HOST_KEY)).thenReturn("temp");
         
         final MutableClusterView mutableClusterView = mock(MutableClusterView.class);
         final HashedDatastore hashedDatastore = mock(HashedDatastore.class);
         
         when(hashedDatastore.getByHash(anyString())).thenReturn(new StoredElementImpl("", null));
         
         final List<InetAddress> allowedAddresses = new LinkedList<InetAddress>();
         allowedAddresses.add(InetAddress.getByName("10.1.1.1"));
         
         dac = new DatastoreAccessControl(allowedAddresses, false);
         logicHandler = new DatastoreFilterLogicHandler(mutableClusterView, "localhost", hashedDatastore, dac);
      }

      @Test
      public void shouldRejectHostsWithoutAccess() {
         ReadableHttpServletResponse response = mock(ReadableHttpServletResponse.class);
         
         final FilterDirector actual = logicHandler.handleRequest(mockRequest, response);
         
         assertEquals("", FilterAction.RETURN, actual.getFilterAction());
         assertEquals("", HttpStatusCode.FORBIDDEN, actual.getResponseStatus());
      }

      @Test
      public void shouldAllowHostsAddressesListedInTheHostACL() {
         when(mockRequest.getRemoteHost()).thenReturn("10.1.1.1");
         
         ReadableHttpServletResponse response = mock(ReadableHttpServletResponse.class);
         
         final FilterDirector actual = logicHandler.handleRequest(mockRequest, response);
         
         assertEquals("", FilterAction.RETURN, actual.getFilterAction());
         assertEquals("", HttpStatusCode.NOT_FOUND, actual.getResponseStatus());
      }
   }
}
