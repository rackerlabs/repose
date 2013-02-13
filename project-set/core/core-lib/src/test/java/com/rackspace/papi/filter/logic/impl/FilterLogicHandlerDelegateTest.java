package com.rackspace.papi.filter.logic.impl;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.FilterLogicHandler;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

import static org.mockito.Mockito.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class FilterLogicHandlerDelegateTest {

   @Ignore
   public static class TestParent {

      protected HttpServletRequest servletRequest;
      protected HttpServletResponse servletResponse;
      protected FilterChain filterChain;
      protected FilterLogicHandler filterLogicHandler;
      protected FilterLogicHandlerDelegate filter;
      protected FilterDirector filterDirector;

      @Before
      public void beforeAll() {
         servletRequest = mock(HttpServletRequest.class);
         when(servletRequest.getHeaderNames()).thenReturn(Collections.enumeration(Collections.EMPTY_LIST));

         servletResponse = mock(HttpServletResponse.class);

         filterChain = mock(FilterChain.class);

         filter = new FilterLogicHandlerDelegate(servletRequest, servletResponse, filterChain);

         filterLogicHandler = mock(FilterLogicHandler.class);

         filterDirector = mock(FilterDirector.class);
         when(filterLogicHandler.handleRequest(any(HttpServletRequest.class), any(ReadableHttpServletResponse.class))).thenReturn(filterDirector);
         when(filterLogicHandler.handleResponse(any(HttpServletRequest.class), any(ReadableHttpServletResponse.class))).thenReturn(filterDirector);
      }
   }

   public static class WhenPassingRequests extends TestParent {

      @Test
      public void shouldPassOriginalRequestAndResponseObjectsWhenFilterActionIsNotSet() throws Exception {
         when(filterDirector.getFilterAction()).thenReturn(FilterAction.NOT_SET);

         filter.doFilter(filterLogicHandler);

         verify(filterLogicHandler, times(1)).handleRequest(any(MutableHttpServletRequest.class), any(MutableHttpServletResponse.class));
         verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
         verify(filterLogicHandler, never()).handleResponse(any(MutableHttpServletRequest.class), any(MutableHttpServletResponse.class));
         
         verify(filterDirector, never()).applyTo(any(MutableHttpServletRequest.class));
         verify(filterDirector, never()).applyTo(any(MutableHttpServletResponse.class));
      }

      @Test
      public void shouldPassRequests() throws Exception {
         when(filterDirector.getFilterAction()).thenReturn(FilterAction.PASS);

         filter.doFilter(filterLogicHandler);

         verify(filterLogicHandler, times(1)).handleRequest(any(MutableHttpServletRequest.class), any(MutableHttpServletResponse.class));
         verify(filterChain, times(1)).doFilter(any(MutableHttpServletRequest.class), any(MutableHttpServletResponse.class));
         verify(filterLogicHandler, never()).handleResponse(any(MutableHttpServletRequest.class), any(MutableHttpServletResponse.class));
         
         verify(filterDirector, times(1)).applyTo(any(MutableHttpServletRequest.class));
         verify(filterDirector, never()).applyTo(any(MutableHttpServletResponse.class));
      }
   }

   public static class WhenProcessingResponses extends TestParent {

      @Test
      public void shouldProcessResponse() throws Exception {
         when(filterDirector.getFilterAction()).thenReturn(FilterAction.PROCESS_RESPONSE);

         filter.doFilter(filterLogicHandler);

         verify(filterLogicHandler, times(1)).handleRequest(any(MutableHttpServletRequest.class), any(MutableHttpServletResponse.class));
         verify(filterChain, times(1)).doFilter(any(MutableHttpServletRequest.class), any(MutableHttpServletResponse.class));
         verify(filterLogicHandler, times(1)).handleResponse(any(MutableHttpServletRequest.class), any(MutableHttpServletResponse.class));

         verify(filterDirector, times(1)).applyTo(any(MutableHttpServletRequest.class));
         verify(filterDirector, times(1)).applyTo(any(MutableHttpServletResponse.class));
      }
   }

   public static class WhenReturningResponses extends TestParent {

      @Test
      public void shouldReturnModifiedResponse() throws Exception {
         when(filterDirector.getFilterAction()).thenReturn(FilterAction.RETURN);

         filter.doFilter(filterLogicHandler);

         verify(filterLogicHandler, times(1)).handleRequest(any(MutableHttpServletRequest.class), any(MutableHttpServletResponse.class));
         verify(filterChain, never()).doFilter(any(MutableHttpServletRequest.class), any(MutableHttpServletResponse.class));
         verify(filterLogicHandler, never()).handleResponse(any(MutableHttpServletRequest.class), any(MutableHttpServletResponse.class));

         verify(filterDirector, never()).applyTo(any(MutableHttpServletRequest.class));
         verify(filterDirector, times(1)).applyTo(any(MutableHttpServletResponse.class));
      }
   }
}