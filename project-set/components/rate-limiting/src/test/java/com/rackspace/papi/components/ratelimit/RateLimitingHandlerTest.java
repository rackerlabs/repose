package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.ratelimit.cache.RateLimitCache;
import com.rackspace.papi.components.ratelimit.config.RateLimitingConfiguration;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.impl.StoredElementImpl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class RateLimitingHandlerTest extends RateLimitingTestSupport {
   private static Enumeration<String> createStringEnumeration(String... names) {
      Vector<String> namesCollection = new Vector<String>(names.length);

      namesCollection.addAll(Arrays.asList(names));

      return namesCollection.elements();
   }

   public static class WhenMakingInvalidRequests extends TestParent {

      @Test
      public void shouldReturnUnauthorizedWhenUserInformationIsMissing() {
         final FilterDirector director = handlerFactory.newHandler().handleRequest(mockedRequest, null);

         assertEquals("FilterDirectory must return on rate limiting failure", FilterAction.RETURN, director.getFilterAction());
         assertEquals("Must return 401 if the user has not been identified", HttpStatusCode.UNAUTHORIZED, director.getResponseStatus());
      }
   }

   public static class WhenMakingValidRequests extends TestParent {

      @Before
      public void standUp() {
         List<String> headerValues = new LinkedList<String>();
         headerValues.add("group-4");
         headerValues.add("group-2");
         headerValues.add("group-1");
         headerValues.add("group-3");

         when(mockedRequest.getHeaders(PowerApiHeader.GROUPS.toString())).thenReturn(Collections.enumeration(headerValues));

         headerValues = new LinkedList<String>();
         headerValues.add("that other user;q=0.5");
         headerValues.add("127.0.0.1;q=0.1");

         when(mockedRequest.getHeaders(PowerApiHeader.USER.toString())).thenReturn(Collections.enumeration(headerValues));

         when(mockedRequest.getHeader(PowerApiHeader.USER.toString())).thenReturn("127.0.0.1;q=0.1");
         when(mockedRequest.getHeader(PowerApiHeader.GROUPS.toString())).thenReturn("group-1");
      }

      @Test
      public void shouldPassValidRequests() {
         when(mockedRequest.getHeaderNames()).thenReturn(createStringEnumeration("Accept"));
         when(mockedRequest.getMethod()).thenReturn("GET");
         when(mockedRequest.getRequestURI()).thenReturn("/v1.0/12345/resource");
         when(mockedRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/v1.0/12345/resource"));
         when(mockedRequest.getHeader("Accept")).thenReturn(MimeType.APPLICATION_JSON.toString());
         when(mockedRequest.getHeaders("accept")).thenReturn(createStringEnumeration(MimeType.APPLICATION_JSON.toString()));
         final FilterDirector director = handlerFactory.newHandler().handleRequest(mockedRequest, null);

         assertEquals("Filter must pass valid, non-limited requests", FilterAction.PASS, director.getFilterAction());
      }

      @Test
      public void shouldProcessResponseWhenAbsoluteLimitsIntegrationIsEnabled() {
         when(mockedRequest.getMethod()).thenReturn("GET");
         when(mockedRequest.getRequestURI()).thenReturn("/v1.0/limits");
         when(mockedRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/v1.0/limits"));

         final FilterDirector director = handlerFactory.newHandler().handleRequest(mockedRequest, null);

         assertEquals("On successful pass, filter must process response", FilterAction.PROCESS_RESPONSE, director.getFilterAction());
      }

      @Test
      public void shouldChangeAcceptTypeToXmlWhenJsonAbsoluteLimitsIsRequested() {
         when(mockedRequest.getMethod()).thenReturn("GET");
         when(mockedRequest.getRequestURI()).thenReturn("/v1.0/limits");
         when(mockedRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/v1.0/limits"));
         when(mockedRequest.getHeaders("Accept")).thenReturn(Collections.enumeration(Collections.singleton(MimeType.APPLICATION_JSON.toString())));

         final FilterDirector director = handlerFactory.newHandler().handleRequest(mockedRequest, null);

         assertTrue("Filter Director is set to add an accept type header", director.requestHeaderManager().headersToAdd().containsKey("accept"));
         assertTrue("Filter Director is set to remove the accept type header", director.requestHeaderManager().headersToRemove().contains("accept"));
         assertTrue("Filter Director is set to add application/xml to the accept header",
                 director.requestHeaderManager().headersToAdd().get("accept").toArray()[0].toString().equals(MimeType.APPLICATION_XML.getMimeType()));
      }

      @Test
      public void shouldRejectDescribeLimitsCallwith406() {
         when(mockedRequest.getMethod()).thenReturn("GET");
         when(mockedRequest.getRequestURI()).thenReturn("/v1.0/limits");
         when(mockedRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/v1.0/limits"));
         when(mockedRequest.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singleton("Accept")));
         when(mockedRequest.getHeaders("accept")).thenReturn(Collections.enumeration(Collections.singleton("leqz")));
         when(mockedRequest.getHeaders("Accept")).thenReturn(Collections.enumeration(Collections.singleton("leqz")));

         final FilterDirector director = handlerFactory.newHandler().handleRequest(mockedRequest, null);

         assertEquals("On rejected media type, filter must return a response", FilterAction.RETURN, director.getFilterAction());
         assertEquals("On rejected media type, returned status code must be 406", HttpStatusCode.NOT_ACCEPTABLE, director.getResponseStatus());
      }

      @Test
      public void shouldDescribeLimitsCallWithEmptyAcceptType() {
         when(mockedRequest.getMethod()).thenReturn("GET");
         when(mockedRequest.getRequestURI()).thenReturn("/v1.0/limits");
         when(mockedRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/v1.0/limits"));
         when(mockedRequest.getHeaders("Accept")).thenReturn(Collections.enumeration(Collections.singleton("")));

         final FilterDirector director = handlerFactory.newHandler().handleRequest(mockedRequest, null);

         assertEquals("On rejected media type, filter must return a response", FilterAction.PROCESS_RESPONSE, director.getFilterAction());
         assertTrue("Filter Director is set to add application/xml to the accept header",
                 director.requestHeaderManager().headersToAdd().get("accept").toArray()[0].toString().equals(MimeType.APPLICATION_XML.getMimeType()));
      }
   }

   @Ignore
   public static class TestParent {

      protected RateLimitingHandlerFactory handlerFactory;
      protected HttpServletRequest mockedRequest;
      protected ReadableHttpServletResponse mockedResponse;
      protected RateLimiterBuilder rlBuilder;

      @Before
      public void beforeAny() {
         final Datastore datastoreMock = mock(Datastore.class);
         when(datastoreMock.get(anyString())).thenReturn(new StoredElementImpl("key", null));

         rlBuilder = mock(RateLimiterBuilder.class);
         when(rlBuilder.buildRateLimiter(any(RateLimitCache.class), any(Map.class), any(RateLimitingConfiguration.class))).thenAnswer(new Answer<RateLimiter>() {

            @Override
            public RateLimiter answer(InvocationOnMock invocation) throws Throwable {
               return mock(RateLimiter.class);
            }
         });

         handlerFactory = new RateLimitingHandlerFactory(datastoreMock, rlBuilder);
         handlerFactory.configurationUpdated(defaultRateLimitingConfiguration());

         mockedRequest = mock(HttpServletRequest.class);
         mockedResponse = mock(ReadableHttpServletResponse.class);
      }
   }
}
