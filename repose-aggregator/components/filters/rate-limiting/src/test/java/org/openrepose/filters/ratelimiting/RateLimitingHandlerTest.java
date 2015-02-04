package org.openrepose.filters.ratelimiting;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.http.header.HeaderName;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.services.datastore.DatastoreService;
import org.openrepose.services.datastore.Patch;
import org.openrepose.services.datastore.distributed.DistributedDatastore;
import org.openrepose.services.ratelimit.cache.CachedRateLimit;
import org.openrepose.services.ratelimit.cache.UserRateLimit;
import org.openrepose.services.ratelimit.config.ConfiguredRatelimit;
import org.openrepose.services.ratelimit.config.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
      assertEquals("Must return 401 if the user has not been identified", HttpServletResponse.SC_UNAUTHORIZED, director.getResponseStatusCode());
    }
  }

  public static class WhenMakingValidRequests extends TestParent {
      private final ConfiguredRatelimit defaultConfig = new ConfiguredRatelimit();

    @Before
    public void setup() {
      List<String> headerNames = new ArrayList<String>();
      headerNames.add(PowerApiHeader.GROUPS.toString());
      headerNames.add(PowerApiHeader.USER.toString());
      headerNames.add("Accept");

        defaultConfig.setId("one");
        defaultConfig.setUri(".*");
        defaultConfig.setUriRegex(".*");
        defaultConfig.getHttpMethods().add(HttpMethod.GET);
        defaultConfig.setValue(10);
        defaultConfig.setUnit(org.openrepose.services.ratelimit.config.TimeUnit.MINUTE);

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
      HashMap<String, CachedRateLimit> limitMap = new HashMap<String, CachedRateLimit>();
      CachedRateLimit cachedRateLimit = new CachedRateLimit(defaultConfig);
      limitMap.put("252423958:46792755", cachedRateLimit);
      when(datastore.patch(any(String.class), any(Patch.class), anyInt(), any(TimeUnit.class))).thenReturn(new UserRateLimit(limitMap));

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

      assertTrue("Filter Director is set to add an accept type header", director.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("accept")));
      assertTrue("Filter Director is set to remove the accept type header", director.requestHeaderManager().headersToRemove().contains(HeaderName.wrap("accept")));
      assertTrue("Filter Director is set to add application/xml to the accept header",
              director.requestHeaderManager().headersToAdd().get(HeaderName.wrap("accept")).toArray()[0].toString().equals(MimeType.APPLICATION_XML.getMimeType()));
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
      assertEquals("On rejected media type, returned status code must be 406", HttpServletResponse.SC_NOT_ACCEPTABLE, director.getResponseStatusCode());
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
              director.requestHeaderManager().headersToAdd().get(HeaderName.wrap("accept")).toArray()[0].toString().equals(MimeType.APPLICATION_XML.getMimeType()));
    }
  }

  @Ignore
  public static class TestParent {

    protected RateLimitingHandlerFactory handlerFactory;
    protected HttpServletRequest mockedRequest;
    protected ReadableHttpServletResponse mockedResponse;
    protected DistributedDatastore datastore;

    @Before
    public void beforeAny() throws Exception {
      datastore = mock(DistributedDatastore.class);
      final DatastoreService service = mock(DatastoreService.class);

      when(service.getDistributedDatastore()).thenReturn(datastore);

      handlerFactory = new RateLimitingHandlerFactory(service);
      handlerFactory.configurationUpdated(defaultRateLimitingConfiguration());

      mockedRequest = mock(HttpServletRequest.class);
      mockedResponse = mock(ReadableHttpServletResponse.class);
    }

  }
}
