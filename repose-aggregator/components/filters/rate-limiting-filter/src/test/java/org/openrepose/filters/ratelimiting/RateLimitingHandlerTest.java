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
import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.commons.utils.servlet.filter.FilterAction;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.commons.utils.servlet.http.HttpServletResponseWrapper;
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
import org.springframework.mock.web.MockHttpServletRequest;

import com.google.common.base.Optional;

import javax.servlet.http.HttpServletResponse;
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

@RunWith(Enclosed.class)
public class RateLimitingHandlerTest extends RateLimitingTestSupport {

    public static class WhenMakingInvalidRequests extends TestParent {

        @Test
        public void shouldReturnUnauthorizedWhenUserInformationIsMissing() {
            FilterAction filterAction = newHandler().handleRequest(new HttpServletRequestWrapper(mockedRequest), mockedResponse);

            assertEquals("Handler must return on rate limiting failure", FilterAction.RETURN, filterAction);
            verify(mockedResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    public static class WhenMakingValidRequests extends TestParent {
        private final ConfiguredRatelimit defaultConfig = new ConfiguredRatelimit();
        private GregorianCalendar splodeDate = new GregorianCalendar(2016, Calendar.APRIL, 4);

        @Before
        public void setup() {
            defaultConfig.setId("one");
            defaultConfig.setUri(".*");
            defaultConfig.setUriRegex(".*");
            defaultConfig.getHttpMethods().add(HttpMethod.GET);
            defaultConfig.setValue(10);
            defaultConfig.setUnit(org.openrepose.core.services.ratelimit.config.TimeUnit.MINUTE);

            mockedRequest.setMethod("GET");

            mockedRequest.addHeader(PowerApiHeader.GROUPS.toString(), "group-1");
            mockedRequest.addHeader(PowerApiHeader.GROUPS.toString(), "group-4");
            mockedRequest.addHeader(PowerApiHeader.GROUPS.toString(), "group-2");
            mockedRequest.addHeader(PowerApiHeader.GROUPS.toString(), "group-3");

            mockedRequest.addHeader(PowerApiHeader.USER.toString(), "127.0.0.1;q=0.1");
            mockedRequest.addHeader(PowerApiHeader.USER.toString(), "that other user;q=0.5");
        }

        @Test
        public void shouldPassValidRequests() {
            mockedRequest.setRequestURI("/v1.0/12345/resource");
            mockedRequest.addHeader("Accept", MimeType.APPLICATION_JSON.toString());
            HashMap<String, CachedRateLimit> limitMap = new HashMap<>();
            CachedRateLimit cachedRateLimit = new CachedRateLimit(defaultConfig);
            limitMap.put("252423958:46792755", cachedRateLimit);
            when(datastore.patch(any(String.class), any(Patch.class), anyInt(), any(TimeUnit.class))).thenReturn(new UserRateLimit(limitMap));

            FilterAction filterAction = newHandler().handleRequest(new HttpServletRequestWrapper(mockedRequest), null);

            assertEquals("Filter must pass valid, non-limited requests", FilterAction.PASS, filterAction);
        }

        @Test
        public void shouldProcessResponseWhenAbsoluteLimitsIntegrationIsEnabled() {
            mockedRequest.setRequestURI("/v1.0/limits");
            mockedRequest.addHeader("Accept", MimeType.APPLICATION_JSON.toString());

            FilterAction filterAction = newHandler().handleRequest(new HttpServletRequestWrapper(mockedRequest), null);

            assertEquals("On successful pass, filter must process response", FilterAction.PROCESS_RESPONSE, filterAction);
        }

        @Test
        public void shouldChangeAcceptTypeToXmlWhenJsonAbsoluteLimitsIsRequested() {
            mockedRequest.setRequestURI("/v1.0/limits");
            mockedRequest.addHeader("Accept", MimeType.APPLICATION_XML.toString());
            HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(mockedRequest);

            newHandler().handleRequest(wrappedRequest, null);

            assertNotNull("Request has an Accept header", wrappedRequest.getHeader("accept"));
            assertEquals("Request Accept header set to application/xml", MimeType.APPLICATION_XML.getMimeType(), wrappedRequest.getHeader("accept"));
        }

        @Test
        public void shouldRejectDescribeLimitsCallwith406() {
            mockedRequest.setRequestURI("/v1.0/limits");
            mockedRequest.addHeader("Accept", "leqz");

            FilterAction filterAction = newHandler().handleRequest(new HttpServletRequestWrapper(mockedRequest), mockedResponse);

            assertEquals("On rejected media type, filter must return a response", FilterAction.RETURN, filterAction);
            verify(mockedResponse).setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
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
            mockedRequest.setRequestURI("/v1.0/limits");
            mockedRequest.addHeader("Accept", "");
            HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(mockedRequest);

            FilterAction filterAction = newHandler().handleRequest(wrappedRequest, null);

            assertEquals("On rejected media type, filter must return a response", FilterAction.PROCESS_RESPONSE, filterAction);
            assertEquals("Request Accept header set to application/xml", MimeType.APPLICATION_XML.getMimeType(), wrappedRequest.getHeader("accept"));
        }

        @Test
        public void shouldRaiseEventWhenRateLimitBreaches() throws OverLimitException {
            RateLimitingServiceHelper helper = mock(RateLimitingServiceHelper.class);
            mockedRequest.addHeader("Accept", MimeType.APPLICATION_XML.toString());
            RateLimitingHandler handler = new RateLimitingHandler(helper, eventService, true, Optional.of(Pattern.compile(".*")), false, 1);
            OverLimitException exception = new OverLimitException("testmsg", "127.0.0.1;q=0.1", new Date(), 10, "10");
            HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(mockedRequest);
            doThrow(exception).when(helper).trackLimits(wrappedRequest, 1);

            handler.handleRequest(wrappedRequest, mockedResponse);

            verify(eventService).newEvent(eq(RateLimitFilterEvent.OVER_LIMIT), any(OverLimitData.class));
        }
    }

    @Ignore
    public static class TestParent {

        protected MockHttpServletRequest mockedRequest;
        protected HttpServletResponseWrapper mockedResponse;
        protected DistributedDatastore datastore;
        protected EventService eventService;

        @Before
        public void beforeAny() throws Exception {
            datastore = mock(DistributedDatastore.class);
            eventService = mock(EventService.class);
            final DatastoreService service = mock(DatastoreService.class);

            when(service.getDistributedDatastore()).thenReturn(datastore);

            mockedRequest = new MockHttpServletRequest();
            mockedResponse = mock(HttpServletResponseWrapper.class);
        }

        public RateLimitingHandler newHandler() {
            return RateLimitingTestSupport.createHandler(defaultRateLimitingConfiguration(), eventService, datastore);
        }

    }
}
