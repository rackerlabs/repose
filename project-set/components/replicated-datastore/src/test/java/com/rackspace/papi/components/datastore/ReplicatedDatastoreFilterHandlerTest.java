/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
/**
 *
 * @author kush5342
 */
@RunWith(Enclosed.class)
public class ReplicatedDatastoreFilterHandlerTest {
    
    public static FilterDirector director ;
    
       @Ignore
   public static class TestParent {

      protected static final String RESOURCE = "3ae04e3f-164e-ab96-04d2-51aa51104daa";
      protected ReadableHttpServletResponse mockResponse;
      protected ReplicatedDatastoreFilterHandler logicHandler;
     

      @Before
      public void beforeAny() throws Exception {
        
         mockResponse = mock(ReadableHttpServletResponse.class);

         final List<InetAddress> allowedAddresses = new LinkedList<InetAddress>();
         allowedAddresses.add(InetAddress.getByName("10.1.1.1"));

         logicHandler = new ReplicatedDatastoreFilterHandler();
      }

      public HttpServletRequest mockRequestWithMethod(String method, String remoteHost) {
         final HttpServletRequest mockedRequest = mock(HttpServletRequest.class);
         //when(mockedRequest.getRequestURI()).thenReturn(CacheRequest.CACHE_URI_PATH + RESOURCE);
         //director=mock(FilterDirectorImpl.class);
         when(mockedRequest.getMethod()).thenReturn(method);
         when(mockedRequest.getLocalAddr()).thenReturn("localhost");
         when(mockedRequest.getLocalPort()).thenReturn(2101);
         when(mockedRequest.getRemoteHost()).thenReturn(remoteHost);
         //when(mockedRequest.getHeader(DatastoreHeader.HOST_KEY.toString())).thenReturn("temp");

         return mockedRequest;
      }
   }
    
   public static class WhenHandlingRequests extends TestParent {

      @Test
      public void shouldGetCacheObjects() {
         final HttpServletRequest mockGetRequest = mockRequestWithMethod("GET", "10.1.1.1");

         director=logicHandler.handleRequest(mockGetRequest, mockResponse);
         assertEquals(director.getFilterAction(), FilterAction.PASS);
      }

  
   }
   
}
