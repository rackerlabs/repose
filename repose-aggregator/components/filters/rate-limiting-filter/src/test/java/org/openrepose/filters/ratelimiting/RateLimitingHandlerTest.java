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

import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.servlet.filter.FilterAction;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.commons.utils.servlet.http.HttpServletResponseWrapper;
import org.openrepose.core.services.datastore.DatastoreService;
import org.openrepose.core.services.datastore.Patch;
import org.openrepose.core.services.datastore.distributed.DistributedDatastore;
import org.openrepose.core.services.event.EventService;
import org.openrepose.core.services.ratelimit.OverLimitData;
import org.openrepose.core.services.ratelimit.RateLimitFilterEvent;
import org.openrepose.core.services.ratelimit.cache.CachedRateLimit;
import org.openrepose.core.services.ratelimit.cache.UserRateLimit;
import org.openrepose.core.services.ratelimit.config.ConfiguredRatelimit;
import org.openrepose.core.services.ratelimit.config.HttpMethod;
import org.openrepose.core.services.ratelimit.config.RateLimitingConfiguration;
import org.openrepose.core.services.ratelimit.exception.OverLimitException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

public class RateLimitingHandlerTest extends RateLimitingTestSupport {

    private final ConfiguredRatelimit defaultConfig = new ConfiguredRatelimit();
    private MockHttpServletRequest mockedRequest;
    private HttpServletResponseWrapper mockedResponse;
    private Datastore datastore;
    private EventService eventService;


    @Before
    public void setup() {
        datastore = mock(Datastore.class);
        eventService = mock(EventService.class);
        final DatastoreService service = mock(DatastoreService.class);

        when(service.getDistributedDatastore()).thenReturn(datastore);

        mockedRequest = new MockHttpServletRequest();
        mockedResponse = mock(HttpServletResponseWrapper.class);

        defaultConfig.setId("one");
        defaultConfig.setUri(".*");
        defaultConfig.setUriRegex(".*");
        defaultConfig.getHttpMethods().add(HttpMethod.GET);
        defaultConfig.setValue(10);
        defaultConfig.setUnit(org.openrepose.core.services.ratelimit.config.TimeUnit.MINUTE);

        mockedRequest.setMethod("GET");

        mockedRequest.addHeader(PowerApiHeader.GROUPS, "group-1");
        mockedRequest.addHeader(PowerApiHeader.GROUPS, "group-4");
        mockedRequest.addHeader(PowerApiHeader.GROUPS, "group-2");
        mockedRequest.addHeader(PowerApiHeader.GROUPS, "group-3");

        mockedRequest.addHeader(PowerApiHeader.USER, "127.0.0.1;q=0.1");
        mockedRequest.addHeader(PowerApiHeader.USER, "that other user;q=0.5");
    }

    @Test
    public void shouldReturnUnauthorizedWhenUserInformationIsMissing() {
        mockedRequest = new MockHttpServletRequest();
        mockedResponse = mock(HttpServletResponseWrapper.class);

        FilterAction filterAction = newHandler().handleRequest(new HttpServletRequestWrapper(mockedRequest), mockedResponse);

        assertEquals("Handler must return on rate limiting failure", FilterAction.RETURN, filterAction);
        verify(mockedResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }


    @Test
    public void shouldPassValidRequests() {
        mockedRequest.setRequestURI("/v1.0/12345/resource");
        mockedRequest.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
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
        mockedRequest.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE);

        FilterAction filterAction = newHandler().handleRequest(new HttpServletRequestWrapper(mockedRequest), null);

        assertEquals("On successful pass, filter must process response", FilterAction.PROCESS_RESPONSE, filterAction);
    }

    @Test
    public void shouldChangeAcceptTypeToXmlWhenJsonAbsoluteLimitsIsRequested() {
        mockedRequest.setRequestURI("/v1.0/limits");
        mockedRequest.addHeader("Accept", MediaType.APPLICATION_XML_VALUE);
        HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(mockedRequest);

        newHandler().handleRequest(wrappedRequest, null);

        assertNotNull("Request has an Accept header", wrappedRequest.getHeader("accept"));
        assertEquals("Request Accept header set to application/xml", MediaType.APPLICATION_XML_VALUE, wrappedRequest.getHeader("accept"));
    }

    @Test
    public void shouldRejectDescribeLimitsCallAcceptingUnsupportedTypeWith406() {
        mockedRequest.setRequestURI("/v1.0/limits");
        mockedRequest.addHeader("Accept", "leqz");

        FilterAction filterAction = newHandler().handleRequest(new HttpServletRequestWrapper(mockedRequest), mockedResponse);

        assertEquals("On rejected accept type, filter must return a response", FilterAction.RETURN, filterAction);
        verify(mockedResponse).setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
    }

    @Test
    public void shouldReturnJsonDescribeLimitsCallWithNoAcceptTypeNoUpstream() {
        mockedRequest.setRequestURI("/v1.0/limits");
        HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(mockedRequest);

        RateLimitingConfiguration rateLimitingConfiguration = defaultRateLimitingConfiguration();
        rateLimitingConfiguration.getRequestEndpoint().setIncludeAbsoluteLimits(false);
        RateLimitingHandler handler = RateLimitingTestSupport.createHandler(rateLimitingConfiguration, eventService, datastore);

        FilterAction filterAction = handler.handleRequest(wrappedRequest, mockedResponse);

        assertEquals("On no accept type, filter should return", FilterAction.RETURN, filterAction);
        verify(mockedResponse).setContentType(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    public void shouldRejectDescribeLimitsCallAcceptingAZeroQualitySupportedTypeWith406() {
        mockedRequest.setRequestURI("/v1.0/limits");
        mockedRequest.addHeader("Accept", "application/xml;q=0.0");

        FilterAction filterAction = newHandler().handleRequest(new HttpServletRequestWrapper(mockedRequest), mockedResponse);

        assertEquals("On rejected accept type, filter should return", FilterAction.RETURN, filterAction);
        verify(mockedResponse).setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
    }

    @Test
    public void shouldReturnJsonDescribeLimitsCallWhenHighestQualitySupportedType() {
        mockedRequest.setRequestURI("/v1.0/limits");
        mockedRequest.addHeader("Accept", "application/xml;q=0.5, application/json;q=0.8, text/xml");

        RateLimitingConfiguration rateLimitingConfiguration = defaultRateLimitingConfiguration();
        rateLimitingConfiguration.getRequestEndpoint().setIncludeAbsoluteLimits(false);
        RateLimitingHandler handler = RateLimitingTestSupport.createHandler(rateLimitingConfiguration, eventService, datastore);

        FilterAction filterAction = handler.handleRequest(new HttpServletRequestWrapper(mockedRequest), mockedResponse);

        assertEquals("On multiple supported accept types, filter should process response must return", FilterAction.RETURN, filterAction);
        verify(mockedResponse).setContentType(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    public void shouldReturnJsonDescribeLimitsCallWhenMostSpecificSupportedType() {
        mockedRequest.setRequestURI("/v1.0/limits");
        mockedRequest.addHeader("Accept", "application/*, application/json");

        RateLimitingConfiguration rateLimitingConfiguration = defaultRateLimitingConfiguration();
        rateLimitingConfiguration.getRequestEndpoint().setIncludeAbsoluteLimits(false);
        RateLimitingHandler handler = RateLimitingTestSupport.createHandler(rateLimitingConfiguration, eventService, datastore);

        FilterAction filterAction = handler.handleRequest(new HttpServletRequestWrapper(mockedRequest), mockedResponse);

        assertEquals("On multiple supported accept types, filter should process response must return", FilterAction.RETURN, filterAction);
        verify(mockedResponse).setContentType(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    public void shouldReturnXmlDescribeLimitsCallWhenJsonIsMostSpecificSupportedButNotHighestQualityType() {
        mockedRequest.setRequestURI("/v1.0/limits");
        mockedRequest.addHeader("Accept", "application/*;q=0.8, application/json;q=0.5");

        RateLimitingConfiguration rateLimitingConfiguration = defaultRateLimitingConfiguration();
        rateLimitingConfiguration.getRequestEndpoint().setIncludeAbsoluteLimits(false);
        RateLimitingHandler handler = RateLimitingTestSupport.createHandler(rateLimitingConfiguration, eventService, datastore);

        FilterAction filterAction = handler.handleRequest(new HttpServletRequestWrapper(mockedRequest), mockedResponse);

        assertEquals("On multiple supported accept types, filter should process response must return", FilterAction.RETURN, filterAction);
        verify(mockedResponse).setContentType(MediaType.APPLICATION_XML_VALUE);
    }

    @Test
    public void shouldRaiseEventWhenRateLimitBreaches() throws OverLimitException {
        RateLimitingServiceHelper helper = mock(RateLimitingServiceHelper.class);
        mockedRequest.addHeader("Accept", MediaType.APPLICATION_XML_VALUE);
        RateLimitingHandler handler = new RateLimitingHandler(helper, eventService, true, Optional.of(Pattern.compile(".*")), false, 1);
        OverLimitException exception = new OverLimitException("testmsg", "127.0.0.1;q=0.1", new Date(), 10, "10");
        HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(mockedRequest);
        doThrow(exception).when(helper).trackLimits(wrappedRequest, 1);

        handler.handleRequest(wrappedRequest, mockedResponse);

        verify(eventService).newEvent(eq(RateLimitFilterEvent.OVER_LIMIT), any(OverLimitData.class));
    }

    private RateLimitingHandler newHandler() {
        return RateLimitingTestSupport.createHandler(defaultRateLimitingConfiguration(), eventService, datastore);
    }

}
