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

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.openrepose.commons.utils.http.ExtendedHttpHeader;
import org.openrepose.commons.utils.http.OpenStackServiceHeader;
import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.http.header.SplittableHeaderUtil;
import org.openrepose.commons.utils.io.BufferedServletInputStream;
import org.openrepose.commons.utils.io.RawInputStreamReader;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.commons.utils.servlet.http.HttpServletResponseWrapper;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.powerfilter.filtercontext.FilterContext;
import org.openrepose.powerfilter.intrafilterlogging.RequestLog;
import org.openrepose.powerfilter.intrafilterlogging.ResponseLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.openrepose.commons.utils.servlet.http.ResponseMode.*;

/**
 * @author fran
 *         <p/>
 *         Cases to handle/test: 1. There are no filters in our chain but some in container's 2. There are filters in our chain
 *         and in container's 3. There are no filters in our chain or container's 4. There are filters in our chain but none in
 *         container's 5. If one of our filters breaks out of the chain (i.e. it doesn't call doFilter), then we shouldn't call
 *         doFilter on the container's filter chain. 6. If one of the container's filters breaks out of the chain then our chain
 *         should unwind correctly
 */
// @TODO: This class is OBE'd with REP-7231
@Deprecated
public class PowerFilterChain implements FilterChain {

    private static final Logger LOG = LoggerFactory.getLogger(PowerFilterChain.class);
    private static final Logger INTRAFILTER_LOG = LoggerFactory.getLogger("intrafilter-logging");
    private static final String START_TIME_ATTRIBUTE = "org.openrepose.repose.logging.start.time";
    private static final String INTRAFILTER_UUID = "Intrafilter-UUID";

    private final List<FilterContext> filterChainCopy;
    private final FilterChain containerFilterChain;
    private final PowerFilterRouter router;
    private final Optional<MetricsService> metricsService;
    private final SplittableHeaderUtil splittabelHeaderUtil;
    private List<FilterContext> currentFilters;
    private int position;
    private RequestTracer tracer = null;
    private boolean filterChainAvailable;
    private Optional<String> bypassUrl;

    public PowerFilterChain(List<FilterContext> filterChainCopy,
                            FilterChain containerFilterChain,
                            PowerFilterRouter router,
                            Optional<MetricsService> metricsService,
                            Optional<String> bypassUrl)
            throws PowerFilterChainException {

        this.filterChainCopy = new LinkedList<>(filterChainCopy);
        this.containerFilterChain = containerFilterChain;
        this.router = router;
        this.metricsService = metricsService;
        this.bypassUrl = bypassUrl;
        splittabelHeaderUtil = new SplittableHeaderUtil(PowerApiHeader.values(), OpenStackServiceHeader.values(),
                ExtendedHttpHeader.values());
    }

    public void startFilterChain(HttpServletRequestWrapper wrappedRequest, HttpServletResponseWrapper wrappedResponse)
            throws IOException, ServletException {

        boolean addTraceHeader = traceRequest(wrappedRequest);
        boolean useTrace = addTraceHeader || metricsService.isPresent();

        tracer = new RequestTracer(useTrace, addTraceHeader);
        currentFilters = getFilterChainForRequest(wrappedRequest);
        filterChainAvailable = isCurrentFilterChainAvailable();
        wrappedRequest.setAttribute("filterChainAvailableForRequest", filterChainAvailable);
        wrappedRequest.setAttribute("http://openrepose.org/requestUrl", wrappedRequest.getRequestURL().toString());

        splitRequestHeaders(wrappedRequest);

        doFilter(wrappedRequest, wrappedResponse);
    }

    private void splitRequestHeaders(HttpServletRequestWrapper request) {
        Collections.list(request.getHeaderNames()).stream()
                .filter(splittabelHeaderUtil::isSplittable)
                .forEach(headerName -> {
                    Enumeration<String> headerValues = request.getHeaders(headerName);
                    request.removeHeader(headerName);
                    splitRequestHeaderValues(headerValues)
                            .forEach(headerValue -> request.addHeader(headerName, headerValue));
                });
    }

    private List<String> splitRequestHeaderValues(Enumeration<String> headerValues) {
        List<String> splitHeaders = new ArrayList<>();
        while (headerValues.hasMoreElements()) {
            String headerValue = headerValues.nextElement();
            String[] splitValues = headerValue.split(",");
            Collections.addAll(splitHeaders, splitValues);
        }
        return splitHeaders;
    }

    /**
     * Find the filters that are applicable to this request based on the criteria specified for each filter and the
     * current request uri.
     * <p/>
     * If a necessary filter is not available, then return an empty filter list.
     */
    private List<FilterContext> getFilterChainForRequest(HttpServletRequestWrapper request) {
        List<FilterContext> filters = new LinkedList<>();
        if (bypassUrl.map(url -> Pattern.compile(url).matcher(request.getRequestURI()).matches()).orElse(false)) {
            LOG.debug("URI: {} matched bypass criteria using empty filter chain", request.getRequestURI());
        } else {
            filterChainCopy.stream()
                .filter(filterContext -> filterContext.shouldRun(request))
                .forEach(filters::add);
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

    private void doReposeFilter(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            FilterContext filterContext) throws IOException, ServletException {
        HttpServletRequest maybeWrappedServletRequest = httpRequest;
        HttpServletResponse maybeWrappedServletResponse = httpResponse;

        try {
            // we don't want to handle trace logging being turned on in the middle of a request, so check upfront
            boolean isIntraFilterLoggingEnabled = INTRAFILTER_LOG.isTraceEnabled() &&
                Optional.ofNullable(MDC.get(PowerApiHeader.TRACE_REQUEST)).isPresent();

            if (isIntraFilterLoggingEnabled) {
                ServletInputStream inputStream = maybeWrappedServletRequest.getInputStream();
                if (!inputStream.markSupported()) {
                    // need to put the input stream into something that supports mark/reset so we can log it
                    ByteArrayOutputStream sourceEntity = new ByteArrayOutputStream();
                    RawInputStreamReader.instance().copyTo(inputStream, sourceEntity);
                    inputStream = new BufferedServletInputStream(new ByteArrayInputStream(sourceEntity.toByteArray()));
                }

                maybeWrappedServletRequest = new HttpServletRequestWrapper(maybeWrappedServletRequest, inputStream);
                maybeWrappedServletResponse = new HttpServletResponseWrapper(
                        maybeWrappedServletResponse,
                        PASSTHROUGH,
                        READONLY);

                INTRAFILTER_LOG.trace(
                        intrafilterRequestLog((HttpServletRequestWrapper) maybeWrappedServletRequest, filterContext));
            }

            filterContext.getFilter().doFilter(maybeWrappedServletRequest, maybeWrappedServletResponse, this);

            if (isIntraFilterLoggingEnabled) {
                // log the response, and give it the request's UUID if the response didn't already have one
                INTRAFILTER_LOG.trace(intrafilterResponseLog(
                        (HttpServletResponseWrapper) maybeWrappedServletResponse,
                        filterContext,
                        maybeWrappedServletRequest.getHeader(INTRAFILTER_UUID)));
            }
        } catch (Exception ex) {
            String filterName = filterContext.getFilter().getClass().getSimpleName();
            LOG.error("Failure in filter: " + filterName + "  -  Reason: " + ex.getMessage(), ex);
            maybeWrappedServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String intrafilterRequestLog(
            HttpServletRequestWrapper wrappedServletRequest,
            FilterContext filterContext) throws IOException {

        // if the request doesn't already have a UUID, give it a new UUID
        if (StringUtils.isEmpty(wrappedServletRequest.getHeader(INTRAFILTER_UUID))) {
            wrappedServletRequest.addHeader(INTRAFILTER_UUID, UUID.randomUUID().toString());
        }

        RequestLog requestLog = new RequestLog(wrappedServletRequest, filterContext.getFilterConfig().getName());

        return convertPojoToJsonString(requestLog);
    }

    private String intrafilterResponseLog(
            HttpServletResponseWrapper wrappedServletResponse,
            FilterContext filterContext,
            String uuid) throws IOException {

        // if the response doesn't already have a UUID, give it the UUID passed to this method
        if (StringUtils.isEmpty(wrappedServletResponse.getHeader(INTRAFILTER_UUID))) {
            wrappedServletResponse.addHeader(INTRAFILTER_UUID, uuid);
        }

        ResponseLog responseLog = new ResponseLog(wrappedServletResponse, filterContext.getFilterConfig().getName());

        return convertPojoToJsonString(responseLog);
    }

    private String convertPojoToJsonString(Object object) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY); //http://stackoverflow.com/a/8395924

        return objectMapper.writeValueAsString(object);
    }

    private void doRouting(HttpServletRequest httpRequest, ServletResponse servletResponse) {
        final HttpServletResponseWrapper wrappedResponse = new HttpServletResponseWrapper(
                (HttpServletResponse) servletResponse,
                MUTABLE,
                PASSTHROUGH
        );

        try {
            if (isResponseOk(wrappedResponse)) {
                containerFilterChain.doFilter(httpRequest, wrappedResponse);
            }
            if (isResponseOk(wrappedResponse)) {
                router.route(new HttpServletRequestWrapper(httpRequest), wrappedResponse);
            }
            splitResponseHeaders(wrappedResponse);
        } catch (Exception ex) {
            LOG.error("Failure in filter within container filter chain. Reason: " + ex.getMessage(), ex);
            wrappedResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            wrappedResponse.commitToResponse();
        }
    }

    private void splitResponseHeaders(HttpServletResponseWrapper httpServletResponseWrapper) {
        httpServletResponseWrapper.getHeaderNames().stream()
                .filter(splittabelHeaderUtil::isSplittable)
                .forEach(headerName -> {
                    Collection<String> splitValues = splitResponseHeaderValues(httpServletResponseWrapper.getHeaders(headerName));
                    httpServletResponseWrapper.removeHeader(headerName);
                    splitValues.stream()
                            .filter(StringUtils::isNotEmpty)
                            .forEach(splitValue -> httpServletResponseWrapper.addHeader(headerName, splitValue));
                });
    }

    private Collection<String> splitResponseHeaderValues(Collection<String> headerValues) {
        List<String> finalValues = new ArrayList<>();
        for (String passedValue : headerValues) {
            String[] splitValues = passedValue.split(",");
            Collections.addAll(finalValues, splitValues);
        }
        return finalValues;
    }

    private void setStartTimeForHttpLogger(long startTime, HttpServletRequest httpRequest) {
        long start = startTime;

        if (startTime == 0) {
            start = System.currentTimeMillis();
        }
        httpRequest.setAttribute(START_TIME_ATTRIBUTE, start);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        if (filterChainAvailable && position < currentFilters.size()) {
            FilterContext filter = currentFilters.get(position++);
            long start = tracer.traceEnter();
            setStartTimeForHttpLogger(start, httpRequest);
            doReposeFilter(httpRequest, httpResponse, filter);
            long delay = tracer.traceExit((HttpServletResponse) servletResponse, filter.getFilterConfig().getName());
            updateTimer(filter.getFilterConfig().getName(), delay);
        } else {
            tracer.traceEnter();
            doRouting(httpRequest, servletResponse);
            long delay = tracer.traceExit((HttpServletResponse) servletResponse, "route");
            updateTimer("route", delay);
        }
    }

    private void updateTimer(String name, long durationMillis) {
        metricsService.ifPresent(ms ->
            ms.getRegistry()
                .timer(MetricRegistry.name("org.openrepose.core.FilterProcessingTime.Delay", name))
                .update(durationMillis, TimeUnit.MILLISECONDS)
        );
    }
}
