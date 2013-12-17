package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.components.datastore.distributed.DistributedDatastore;
import com.rackspace.papi.components.datastore.StoredElementImpl;
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
      List<String> headerNames = new ArrayList<String>();
      headerNames.add(PowerApiHeader.GROUPS.toString());
      headerNames.add(PowerApiHeader.USER.toString());
      headerNames.add("Accept");
      
      
      when(mockedRequest.getHeaderNames()).thenReturn(Collections.enumeration(headerNames));

      when(mockedRequest.getHeaders(PowerApiHeader.GROUPS.toString())).thenAnswer(new Answer<Object>() {
        public Object answer(InvocationOnMock invocation) throws Throwable {
          List<String> headerValues = new LinkedList<String>();
          headerValues.add("group-4");
          headerValues.add("group-2");
          headerValues.add("group-1");
          headerValues.add("group-3");

          return Collections.enumeration(headerValues);
        }
      });

      when(mockedRequest.getHeaders(PowerApiHeader.USER.toString())).thenAnswer(new Answer<Object>() {
        public Object answer(InvocationOnMock invocation) throws Throwable {
          List<String> headerValues = new LinkedList<String>();
          headerValues.add("that other user;q=0.5");
          headerValues.add("127.0.0.1;q=0.1");

          return Collections.enumeration(headerValues);
        }
      });

      when(mockedRequest.getHeader(PowerApiHeader.USER.toString())).thenReturn("127.0.0.1;q=0.1");
      when(mockedRequest.getHeader(PowerApiHeader.GROUPS.toString())).thenReturn("group-1");
    }

    @Test
    public void shouldPassValidRequests() {
      when(mockedRequest.getHeaderNames()).thenAnswer(new Answer<Object>() {
        public Object answer(InvocationOnMock invocation) throws Throwable {
          return createStringEnumeration("Accept", PowerApiHeader.USER.toString(), PowerApiHeader.GROUPS.toString());
        }
      });

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
      when(mockedRequest.getHeaderNames()).thenAnswer(new Answer<Object>() {
        public Object answer(InvocationOnMock invocation) throws Throwable {
          return createStringEnumeration("Accept", PowerApiHeader.USER.toString(), PowerApiHeader.GROUPS.toString());
        }
      });

      when(mockedRequest.getMethod()).thenReturn("GET");
      when(mockedRequest.getRequestURI()).thenReturn("/v1.0/limits");
      when(mockedRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/v1.0/limits"));
      when(mockedRequest.getHeader("Accept")).thenReturn(MimeType.APPLICATION_JSON.toString());
      when(mockedRequest.getHeaders("accept")).thenReturn(createStringEnumeration(MimeType.APPLICATION_JSON.toString()));

      final FilterDirector director = handlerFactory.newHandler().handleRequest(mockedRequest, null);

      assertEquals("On successful pass, filter must process response", FilterAction.PROCESS_RESPONSE, director.getFilterAction());
    }

    @Test
    public void shouldChangeAcceptTypeToXmlWhenJsonAbsoluteLimitsIsRequested() {
      when(mockedRequest.getHeaderNames()).thenAnswer(new Answer<Object>() {
        public Object answer(InvocationOnMock invocation) throws Throwable {
          return createStringEnumeration("Accept", PowerApiHeader.USER.toString(), PowerApiHeader.GROUPS.toString());
        }
      });

      when(mockedRequest.getMethod()).thenReturn("GET");
      when(mockedRequest.getRequestURI()).thenReturn("/v1.0/limits");
      when(mockedRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/v1.0/limits"));
      when(mockedRequest.getHeaders("Accept")).thenReturn(Collections.enumeration(Collections.singleton(MimeType.APPLICATION_XML.toString())));
      when(mockedRequest.getHeaders("accept")).thenReturn(createStringEnumeration(MimeType.APPLICATION_XML.toString()));

      final FilterDirector director = handlerFactory.newHandler().handleRequest(mockedRequest, null);

      assertTrue("Filter Director is set to add an accept type header", director.requestHeaderManager().headersToAdd().containsKey("accept"));
      assertTrue("Filter Director is set to remove the accept type header", director.requestHeaderManager().headersToRemove().contains("accept"));
      assertTrue("Filter Director is set to add application/xml to the accept header",
              director.requestHeaderManager().headersToAdd().get("accept").toArray()[0].toString().equals(MimeType.APPLICATION_XML.getMimeType()));
    }

    @Test
    public void shouldRejectDescribeLimitsCallwith406() {
      when(mockedRequest.getHeaderNames()).thenAnswer(new Answer<Object>() {
        public Object answer(InvocationOnMock invocation) throws Throwable {
          return createStringEnumeration("Accept", PowerApiHeader.USER.toString(), PowerApiHeader.GROUPS.toString());
        }
      });

      when(mockedRequest.getMethod()).thenReturn("GET");
      when(mockedRequest.getRequestURI()).thenReturn("/v1.0/limits");
      when(mockedRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/v1.0/limits"));
      when(mockedRequest.getHeaders("accept")).thenReturn(Collections.enumeration(Collections.singleton("leqz")));
      when(mockedRequest.getHeaders("Accept")).thenReturn(Collections.enumeration(Collections.singleton("leqz")));

      final FilterDirector director = handlerFactory.newHandler().handleRequest(mockedRequest, null);

      assertEquals("On rejected media type, filter must return a response", FilterAction.RETURN, director.getFilterAction());
      assertEquals("On rejected media type, returned status code must be 406", HttpStatusCode.NOT_ACCEPTABLE, director.getResponseStatus());
    }

    @Test
    public void shouldDescribeLimitsCallWithEmptyAcceptType() {
      when(mockedRequest.getHeaderNames()).thenAnswer(new Answer<Object>() {
        public Object answer(InvocationOnMock invocation) throws Throwable {
          return createStringEnumeration("Accept", PowerApiHeader.USER.toString(), PowerApiHeader.GROUPS.toString());
        }
      });

      when(mockedRequest.getMethod()).thenReturn("GET");
      when(mockedRequest.getRequestURI()).thenReturn("/v1.0/limits");
      when(mockedRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/v1.0/limits"));
      when(mockedRequest.getHeaders("Accept")).thenReturn(Collections.enumeration(Collections.singleton("")));
      when(mockedRequest.getHeaders("accept")).thenReturn(Collections.enumeration(Collections.singleton("")));

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

    @Before
    public void beforeAny() {
      final DistributedDatastore distDatastoreMock = mock(DistributedDatastore.class);
      final DatastoreService service = mock(DatastoreService.class);

      when(service.getDistributedDatastore()).thenReturn(distDatastoreMock);
      when(distDatastoreMock.get(anyString())).thenReturn(new StoredElementImpl("key", null));

      handlerFactory = new RateLimitingHandlerFactory(service);
      handlerFactory.configurationUpdated(defaultRateLimitingConfiguration());

      mockedRequest = mock(HttpServletRequest.class);
      mockedResponse = mock(ReadableHttpServletResponse.class);
    }

  }
}
