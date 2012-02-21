package com.rackspace.papi.components.serviceauth;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterDirector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class ServiceAuthenticationHandlerTest {

   public static class WhenHandlingRequests {
      private final String CREDENTIALS = "blah";
      private ServiceAuthenticationHandler instance;
      private HttpServletRequest httpRequest;
      private MutableHttpServletRequest request;

      @Before
      public void setUp() {
         instance = new ServiceAuthenticationHandler(CREDENTIALS);
         
         httpRequest = mock(HttpServletRequest.class);
         
         List<String> headers = new ArrayList();
         
         when(httpRequest.getHeaderNames()).thenReturn(Collections.enumeration(headers));
         request = MutableHttpServletRequest.wrap(httpRequest);
      }
 
      @Test
      public void shouldSetAuthHeader() {
         instance.handleRequest(request, null);
         assertEquals("Should set credentials in header", request.getHeader(CommonHttpHeader.AUTHORIZATION.toString()), CREDENTIALS);
      }
   }
   
   public static class WhenHandlingResponses {
      private final String CREDENTIALS = "blah";
      private ServiceAuthenticationHandler instance;
      private HttpServletRequest httpRequest;
      private HttpServletResponse httpResponse;
      private MutableHttpServletRequest request;
      private MutableHttpServletResponse response;

      @Before
      public void setUp() {
         instance = new ServiceAuthenticationHandler(CREDENTIALS);
         
         httpRequest = mock(HttpServletRequest.class);
         httpResponse = mock(HttpServletResponse.class);
         
         List<String> headers = new ArrayList();
         
         when(httpRequest.getHeaderNames()).thenReturn(Collections.enumeration(headers));
         request = MutableHttpServletRequest.wrap(httpRequest);
         response = MutableHttpServletResponse.wrap(httpResponse);
      }

      @Test
      public void shouldHandleNotImplementedResponse() {
         when(httpResponse.getStatus()).thenReturn(HttpStatusCode.NOT_IMPLEMENTED.intValue());
         instance.handleResponse(request, response);
         verify(httpResponse).setStatus(eq(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue()));
      }

      @Test
      public void shouldHandleForbiddenResponseWithNoHeader() {
         when(httpResponse.getStatus()).thenReturn(HttpStatusCode.FORBIDDEN.intValue());
         instance.handleResponse(request, response);
         verify(httpResponse).setStatus(eq(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue()));
      }

      @Test
      public void shouldHandleForbiddenResponseWithDelegatedHeader() {
         when(response.getHeader(eq(CommonHttpHeader.WWW_AUTHENTICATE.toString()))).thenReturn("delegated");
         when(httpResponse.getStatus()).thenReturn(HttpStatusCode.FORBIDDEN.intValue());
         instance.handleResponse(request, response);
         verify(httpResponse).setHeader(eq(CommonHttpHeader.WWW_AUTHENTICATE.toString()), eq((String)null));
      }

      @Test
      public void shouldHandleForbiddenResponseWithNonDelegatedHeader() {
         when(response.getHeader(eq(CommonHttpHeader.WWW_AUTHENTICATE.toString()))).thenReturn("not-delegated");
         when(httpResponse.getStatus()).thenReturn(HttpStatusCode.FORBIDDEN.intValue());
         instance.handleResponse(request, response);
         verify(httpResponse).setStatus(eq(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue()));
      }
   }

}
