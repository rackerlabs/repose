/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.ratelimiting;

import org.junit.Assume;
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
import org.openrepose.core.services.datastore.DatastoreService;
import org.openrepose.core.services.datastore.Patch;
import org.openrepose.core.services.datastore.distributed.DistributedDatastore;
import org.openrepose.core.services.event.common.EventService;
import org.openrepose.core.services.ratelimit.OverLimitData;
import org.openrepose.core.services.ratelimit.RateLimitFilterEvent;
import org.openrepose.core.services.ratelimit.cache.CachedRateLimit;
import org.openrepose.core.services.ratelimit.cache.UserRateLimit;
import org.openrepose.core.services.ratelimit.config.ConfiguredRatelimit;
import org.openrepose.core.services.ratelimit.config.HttpMethod;
import org.openrepose.core.services.ratelimit.exception.OverLimitException;

import com.google.common.base.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.openrepose.core.filter.logic.FilterDirector.SC_UNSUPPORTED_RESPONSE_CODE;

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
        private GregorianCalendar splodeDate = new GregorianCalendar(2017, Calendar.JANUARY, 5);

        @Before
        public void setup() {
            final List<String> headerNames = new ArrayList<>();
            headerNames.add(PowerApiHeader.GROUPS.toString());
            headerNames.add(PowerApiHeader.USER.toString());
            headerNames.add("Accept");

            defaultConfig.setId("one");
            defaultConfig.setUri(".*");
            defaultConfig.setUriRegex(".*");
            defaultConfig.getHttpMethods().add(HttpMethod.GET);
            defaultConfig.setValue(10);
            defaultConfig.setUnit(org.openrepose.core.services.ratelimit.config.TimeUnit.MINUTE);

            when(mockedRequest.getMethod()).thenReturn("GET");
            when(mockedRequest.getHeaderNames()).thenAnswer(new Answer<Object>() {
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    return Collections.enumeration(headerNames);
                }
            });

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
            when(mockedRequest.getRequestURI()).thenReturn("/v1.0/12345/resource");
            when(mockedRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/v1.0/12345/resource"));
            when(mockedRequest.getHeader("Accept")).thenReturn(MimeType.APPLICATION_JSON.toString());
            when(mockedRequest.getHeaders("Accept")).thenReturn(createStringEnumeration(MimeType.APPLICATION_JSON.toString()));
            HashMap<String, CachedRateLimit> limitMap = new HashMap<String, CachedRateLimit>();
            CachedRateLimit cachedRateLimit = new CachedRateLimit(defaultConfig);
            limitMap.put("252423958:46792755", cachedRateLimit);
            when(datastore.patch(any(String.class), any(Patch.class), anyInt(), any(TimeUnit.class))).thenReturn(new UserRateLimit(limitMap));

            final FilterDirector director = handlerFactory.newHandler().handleRequest(mockedRequest, null);

            assertEquals("Filter must pass valid, non-limited requests", FilterAction.PASS, director.getFilterAction());
        }

        @Test
        public void shouldProcessResponseWhenAbsoluteLimitsIntegrationIsEnabled() {
            when(mockedRequest.getRequestURI()).thenReturn("/v1.0/limits");
            when(mockedRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/v1.0/limits"));
            when(mockedRequest.getHeader("Accept")).thenReturn(MimeType.APPLICATION_JSON.toString());
            when(mockedRequest.getHeaders("Accept")).thenReturn(createStringEnumeration(MimeType.APPLICATION_JSON.toString()));

            final FilterDirector director = handlerFactory.newHandler().handleRequest(mockedRequest, null);

            assertEquals("On successful pass, filter must process response", FilterAction.PROCESS_RESPONSE, director.getFilterAction());
        }

        @Test
        public void shouldChangeAcceptTypeToXmlWhenJsonAbsoluteLimitsIsRequested() {
            when(mockedRequest.getRequestURI()).thenReturn("/v1.0/limits");
            when(mockedRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/v1.0/limits"));
            when(mockedRequest.getHeader("Accept")).thenReturn(MimeType.APPLICATION_XML.toString());
            when(mockedRequest.getHeaders("Accept")).thenReturn(createStringEnumeration(MimeType.APPLICATION_XML.toString()));

            final FilterDirector director = handlerFactory.newHandler().handleRequest(mockedRequest, null);

            assertTrue("Filter Director is set to add an accept type header", director.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("accept")));
            assertTrue("Filter Director is set to remove the accept type header", director.requestHeaderManager().headersToRemove().contains(HeaderName.wrap("accept")));
            assertTrue("Filter Director is set to add application/xml to the accept header",
                    director.requestHeaderManager().headersToAdd().get(HeaderName.wrap("accept")).toArray()[0].toString().equals(MimeType.APPLICATION_XML.getMimeType()));
        }

        @Test
        public void shouldRejectDescribeLimitsCallwith406() {
            when(mockedRequest.getRequestURI()).thenReturn("/v1.0/limits");
            when(mockedRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/v1.0/limits"));
            when(mockedRequest.getHeader("Accept")).thenReturn("leqz");
            when(mockedRequest.getHeaders("Accept")).thenReturn(Collections.enumeration(Collections.singleton("leqz")));

            final FilterDirector director = handlerFactory.newHandler().handleRequest(mockedRequest, null);

            assertEquals("On rejected media type, filter must return a response", FilterAction.RETURN, director.getFilterAction());
            assertEquals("On rejected media type, returned status code must be 406", HttpServletResponse.SC_NOT_ACCEPTABLE, director.getResponseStatusCode());
        }

        // If the accept header is set to a media type that the rate limiting filter cannot handle,
        // the rate limiting filter should return a 406, or a body with some default media type
        // (ignoring the accept header). This test assumes that the latter approach was taken, and
        // that limits are retrieved from the origin service as xml, combined, and returned to the
        // requestor. HTTP/1.0 spec dicatates that the former approach be taken, while HTTP/1.1
        // leaves it as an implementation decision.
        @Test
        public void shouldDescribeLimitsCallWithEmptyAcceptType() {
            Assume.assumeTrue(new Date().getTime() > splodeDate.getTime().getTime());
            when(mockedRequest.getRequestURI()).thenReturn("/v1.0/limits");
            when(mockedRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/v1.0/limits"));
            when(mockedRequest.getHeader("Accept")).thenReturn("");
            when(mockedRequest.getHeaders("Accept")).thenReturn(Collections.enumeration(Collections.singleton("")));

            final FilterDirector director = handlerFactory.newHandler().handleRequest(mockedRequest, null);

            assertEquals("On rejected media type, filter must return a response", FilterAction.PROCESS_RESPONSE, director.getFilterAction());
            assertTrue("Filter Director is set to add application/xml to the accept header",
                    director.requestHeaderManager().headersToAdd().get(HeaderName.wrap("accept")).toArray()[0].toString().equals(MimeType.APPLICATION_XML.getMimeType()));
        }
        public void shouldRaiseEventWhenRateLimitBreaches() throws OverLimitException {
            RateLimitingServiceHelper helper = mock(RateLimitingServiceHelper.class);
            when(mockedRequest.getHeaders("Accept")).thenReturn(Collections.enumeration(Collections.singleton(MimeType.APPLICATION_XML.toString())));
            RateLimitingHandler handler = new RateLimitingHandler(helper, eventService, true, Optional.<Pattern>of(Pattern.compile(".*")), false, 1);
            OverLimitException exception = new OverLimitException("testmsg", "127.0.0.1;q=0.1", new Date(), 10, "10");
            doThrow(exception).when(helper).trackLimits(mockedRequest, 1);
            handler.handleRequest(mockedRequest, mockedResponse);
            verify(eventService).newEvent(eq(RateLimitFilterEvent.OVER_LIMIT), any(OverLimitData.class));
        }

    }

    @Ignore
    public static class TestParent {

        protected RateLimitingHandlerFactory handlerFactory;
        protected HttpServletRequest mockedRequest;
        protected ReadableHttpServletResponse mockedResponse;
        protected DistributedDatastore datastore;
        protected EventService eventService;

        @Before
        public void beforeAny() throws Exception {
            datastore = mock(DistributedDatastore.class);
            eventService = mock(EventService.class);
            final DatastoreService service = mock(DatastoreService.class);

            when(service.getDistributedDatastore()).thenReturn(datastore);

            handlerFactory = new RateLimitingHandlerFactory(service, eventService);
            handlerFactory.configurationUpdated(defaultRateLimitingConfiguration());

            mockedRequest = mock(HttpServletRequest.class);
            mockedResponse = mock(ReadableHttpServletResponse.class);
        }

    }
}
