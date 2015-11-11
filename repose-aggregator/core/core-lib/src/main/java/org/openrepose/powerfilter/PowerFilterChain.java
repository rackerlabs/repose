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
package org.openrepose.powerfilter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.openrepose.commons.utils.http.ExtendedHttpHeader;
import org.openrepose.commons.utils.http.OpenStackServiceHeader;
import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.http.header.HeaderFieldParser;
import org.openrepose.commons.utils.http.header.HeaderValue;
import org.openrepose.commons.utils.http.header.SplittableHeaderUtil;
import org.openrepose.commons.utils.servlet.http.HttpServletResponseWrapper;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.core.FilterProcessingTime;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.core.services.reporting.metrics.TimerByCategory;
import org.openrepose.powerfilter.filtercontext.FilterContext;
import org.openrepose.powerfilter.intrafilterLogging.RequestLog;
import org.openrepose.powerfilter.intrafilterLogging.ResponseLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.openrepose.commons.utils.servlet.http.ResponseMode.PASSTHROUGH;
import static org.openrepose.commons.utils.servlet.http.ResponseMode.MUTABLE;

/**
 * @author fran
 *         <p/>
 *         Cases to handle/test: 1. There are no filters in our chain but some in container's 2. There are filters in our chain
 *         and in container's 3. There are no filters in our chain or container's 4. There are filters in our chain but none in
 *         container's 5. If one of our filters breaks out of the chain (i.e. it doesn't call doFilter), then we shouldn't call
 *         doFilter on the container's filter chain. 6. If one of the container's filters breaks out of the chain then our chain
 *         should unwind correctly
 */
public class PowerFilterChain implements FilterChain {

    private static final Logger LOG = LoggerFactory.getLogger(PowerFilterChain.class);
    private static final Logger INTRAFILTER_LOG = LoggerFactory.getLogger("intrafilter-logging");
    private static final String START_TIME_ATTRIBUTE = "org.openrepose.repose.logging.start.time";
    private static final String INTRAFILTER_UUID = "Intrafilter-UUID";

    private final List<FilterContext> filterChainCopy;
    private final FilterChain containerFilterChain;
    private final PowerFilterRouter router;
    private List<FilterContext> currentFilters;
    private int position;
    private RequestTracer tracer = null;
    private boolean filterChainAvailable;
    private TimerByCategory filterTimer;
    private final SplittableHeaderUtil splittabelHeaderUtil;

    public PowerFilterChain(List<FilterContext> filterChainCopy,
                            FilterChain containerFilterChain,
                            PowerFilterRouter router,
                            MetricsService metricsService)
            throws PowerFilterChainException {

        this.filterChainCopy = new LinkedList<>(filterChainCopy);
        this.containerFilterChain = containerFilterChain;
        this.router = router;
        if (metricsService != null) {
            filterTimer = metricsService.newTimerByCategory(FilterProcessingTime.class, "Delay", TimeUnit.MILLISECONDS,
                    TimeUnit.MILLISECONDS);
        }
        splittabelHeaderUtil = new SplittableHeaderUtil(PowerApiHeader.values(), OpenStackServiceHeader.values(),
                ExtendedHttpHeader.values());
    }

    public void startFilterChain(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) servletRequest;

        boolean addTraceHeader = traceRequest(request);
        boolean useTrace = addTraceHeader || (filterTimer != null);

        tracer = new RequestTracer(useTrace, addTraceHeader);
        currentFilters = getFilterChainForRequest(request.getRequestURI());
        filterChainAvailable = isCurrentFilterChainAvailable();
        servletRequest.setAttribute("filterChainAvailableForRequest", filterChainAvailable);
        servletRequest.setAttribute("http://openrepose.org/requestUrl", ((HttpServletRequest) servletRequest).getRequestURL().toString());
        servletRequest.setAttribute("http://openrepose.org/queryParams", servletRequest.getParameterMap());
        MutableHttpServletRequest wrappedRequest = MutableHttpServletRequest.wrap((HttpServletRequest) servletRequest);
        splitRequestHeaders(wrappedRequest);

        doFilter(wrappedRequest, servletResponse);
    }

    private void splitRequestHeaders(MutableHttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (splittabelHeaderUtil.isSplitable(headerName)) {
                List<HeaderValue> splitValues = splitRequestHeaderValues(headerName, request.getHeaders(headerName));
                List<HeaderValue> headerValuesList = request.getRequestValues().getHeaders().getHeaderValues(headerName);
                headerValuesList.clear();
                headerValuesList.addAll(splitValues);
            }
        }
    }

    private List<HeaderValue> splitRequestHeaderValues(String headerName, Enumeration<String> headerValues) {
        List<HeaderValue> splitHeaders = new ArrayList<>();
        while (headerValues.hasMoreElements()) {
            String headerValue = headerValues.nextElement();
            String[] splitValues = headerValue.split(",");
            for (String splitValue : splitValues) {
                splitHeaders.addAll(new HeaderFieldParser(splitValue, headerName).parse());
            }
        }
        return splitHeaders;
    }

    /**
     * Find the filters that are applicable to this request based on the uri-regex specified for each filter and the
     * current request uri.
     * <p/>
     * If a necessary filter is not available, then return an empty filter list.
     *
     * @param uri
     * @return
     */
    private List<FilterContext> getFilterChainForRequest(String uri) {
        List<FilterContext> filters = new LinkedList<>();
        for (FilterContext filter : filterChainCopy) {
            if (filter.getUriPattern() == null || filter.getUriPattern().matcher(uri).matches()) {
                filters.add(filter);
            }
        }

        return filters;
    }

    private boolean traceRequest(HttpServletRequest request) {
        return request.getHeader("X-Trace-Request") != null;
    }

    private boolean isCurrentFilterChainAvailable() {
        boolean result = true;

        for (FilterContext filter : currentFilters) {
            if (!filter.isFilterAvailable()) {
                LOG.warn("Filter is not available for processing requests: " + filter.getName());
            }
            result &= filter.isFilterAvailable();
        }

        return result;
    }

    private boolean isResponseOk(HttpServletResponse response) {
        return response.getStatus() < HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    private void doReposeFilter(MutableHttpServletRequest mutableHttpRequest, ServletResponse servletResponse,
                                FilterContext filterContext) throws IOException, ServletException {
        final MutableHttpServletResponse mutableHttpResponse =
                MutableHttpServletResponse.wrap(mutableHttpRequest, (HttpServletResponse) servletResponse);

        mutableHttpResponse.pushOutputStream();

        try {
            if (INTRAFILTER_LOG.isTraceEnabled()) {
                // log the request, and give it a new UUID if it doesn't already have one
                UUID intrafilterUuid = UUID.randomUUID();
                INTRAFILTER_LOG.trace(intrafilterRequestLog(mutableHttpRequest, filterContext, intrafilterUuid));
            }

            filterContext.getFilter().doFilter(mutableHttpRequest, mutableHttpResponse, this);

            if (INTRAFILTER_LOG.isTraceEnabled()) {
                // log the response, and give it the request's UUID if the response didn't already have one
                INTRAFILTER_LOG.trace(intrafilterResponseLog(mutableHttpResponse, filterContext,
                        mutableHttpRequest.getHeader(INTRAFILTER_UUID)));
            }
        } catch (Exception ex) {
            String filterName = filterContext.getFilter().getClass().getSimpleName();
            LOG.error("Failure in filter: " + filterName + "  -  Reason: " + ex.getMessage(), ex);
            mutableHttpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            mutableHttpResponse.popOutputStream();
        }
    }

    private String intrafilterRequestLog(MutableHttpServletRequest mutableHttpRequest,
                                         FilterContext filterContext, UUID uuid) throws IOException {

        // if the request doesn't already have a UUID, give it the UUID passed to this method
        if (StringUtils.isEmpty(mutableHttpRequest.getHeader(INTRAFILTER_UUID))) {
            mutableHttpRequest.addHeader(INTRAFILTER_UUID, uuid.toString());
        }

        RequestLog requestLog = new RequestLog(mutableHttpRequest, filterContext);

        return convertPojoToJsonString(requestLog);
    }

    private String intrafilterResponseLog(MutableHttpServletResponse mutableHttpResponse,
                                          FilterContext filterContext, String uuid) throws IOException {

        // if the response doesn't already have a UUID, give it the UUID passed to this method
        if (StringUtils.isEmpty(mutableHttpResponse.getHeader(INTRAFILTER_UUID))) {
            mutableHttpResponse.addHeader(INTRAFILTER_UUID, uuid);
        }

        ResponseLog responseLog = new ResponseLog(mutableHttpResponse, filterContext);

        return convertPojoToJsonString(responseLog);
    }

    private String convertPojoToJsonString(Object object) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY); //http://stackoverflow.com/a/8395924

        return objectMapper.writeValueAsString(object);
    }

    private void doRouting(MutableHttpServletRequest mutableHttpRequest, ServletResponse servletResponse)
            throws IOException, ServletException {
        final HttpServletResponseWrapper httpServletResponseWrapper = new HttpServletResponseWrapper(
                (HttpServletResponse) servletResponse,
                MUTABLE,
                PASSTHROUGH,
                servletResponse.getOutputStream()
        );

        try {
            if (isResponseOk(httpServletResponseWrapper)) {
                containerFilterChain.doFilter(mutableHttpRequest, httpServletResponseWrapper);
            }
            if (isResponseOk(httpServletResponseWrapper)) {
                router.route(mutableHttpRequest, httpServletResponseWrapper);
            }
            splitResponseHeaders(httpServletResponseWrapper);
            httpServletResponseWrapper.commitToResponse();
        } catch (Exception ex) {
            LOG.error("Failure in filter within container filter chain. Reason: " + ex.getMessage(), ex);
            httpServletResponseWrapper.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void splitResponseHeaders(HttpServletResponseWrapper httpServletResponseWrapper) {
        for (String headerName : httpServletResponseWrapper.getHeaderNames()) {
            if (splittabelHeaderUtil.isSplitable(headerName)) {
                Collection<String> splitValues = splitResponseHeaderValues(httpServletResponseWrapper.getHeaders(headerName));
                httpServletResponseWrapper.removeHeader(headerName);
                for (String splitValue : splitValues) {
                    if (StringUtils.isNotEmpty(splitValue)) {
                        httpServletResponseWrapper.addHeader(headerName, splitValue);
                    }
                }
            }
        }
    }

    private Collection<String> splitResponseHeaderValues(Collection<String> headerValues) {
        List<String> finalValues = new ArrayList<>();
        for (String passedValue : headerValues) {
            String[] splitValues = passedValue.split(",");
            Collections.addAll(finalValues, splitValues);
        }
        return finalValues;
    }

    private void setStartTimeForHttpLogger(long startTime, MutableHttpServletRequest mutableHttpRequest) {
        long start = startTime;

        if (startTime == 0) {
            start = System.currentTimeMillis();
        }
        mutableHttpRequest.setAttribute(START_TIME_ATTRIBUTE, start);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IOException, ServletException {
        final MutableHttpServletRequest mutableHttpRequest =
                MutableHttpServletRequest.wrap((HttpServletRequest) servletRequest);

        if (filterChainAvailable && position < currentFilters.size()) {
            FilterContext filter = currentFilters.get(position++);
            long start = tracer.traceEnter();
            setStartTimeForHttpLogger(start, mutableHttpRequest);
            doReposeFilter(mutableHttpRequest, servletResponse, filter);
            long delay = tracer.traceExit((HttpServletResponse) servletResponse, filter.getFilterConfig().getName());
            if (filterTimer != null) {
                filterTimer.update(filter.getFilterConfig().getName(), delay, TimeUnit.MILLISECONDS);
            }
        } else {
            tracer.traceEnter();
            doRouting(mutableHttpRequest, servletResponse);
            long delay = tracer.traceExit((HttpServletResponse) servletResponse, "route");
            if (filterTimer != null) {
                filterTimer.update("route", delay, TimeUnit.MILLISECONDS);
            }
        }
    }
}
