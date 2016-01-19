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

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.io.stream.ReadLimitReachedException;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.RouteDestination;
import org.openrepose.core.RequestTimeout;
import org.openrepose.core.ResponseCode;
import org.openrepose.core.filter.logic.DispatchPathBuilder;
import org.openrepose.core.filter.routing.DestinationLocation;
import org.openrepose.core.filter.routing.DestinationLocationBuilder;
import org.openrepose.core.services.headers.response.ResponseHeaderService;
import org.openrepose.core.services.reporting.ReportingService;
import org.openrepose.core.services.reporting.metrics.MeterByCategory;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.core.services.reporting.metrics.impl.MeterByCategorySum;
import org.openrepose.core.systemmodel.Destination;
import org.openrepose.core.systemmodel.DestinationCluster;
import org.openrepose.core.systemmodel.DestinationEndpoint;
import org.openrepose.core.systemmodel.ReposeCluster;
import org.openrepose.nodeservice.request.RequestHeaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.net.HttpURLConnection.HTTP_CLIENT_TIMEOUT;

/**
 * This class is kind of gross, but we need to rewrite the whole thing.
 * The factory has to pass in too many things.
 */
public class PowerFilterRouterImpl implements PowerFilterRouter {
    public static Logger LOG = LoggerFactory.getLogger(PowerFilterRouterImpl.class);
    private final DestinationLocationBuilder locationBuilder;
    private final Map<String, Destination> destinations;
    private final ReposeCluster domain;
    private final String defaultDestination;
    private final ServletContext servletContext;
    private final RequestHeaderService requestHeaderService;
    private final ResponseHeaderService responseHeaderService;
    private final MetricsService metricsService;
    private final ConcurrentHashMap<String, MeterByCategory> mapResponseCodes;
    private final ConcurrentHashMap<String, MeterByCategory> mapRequestTimeouts;
    private final ReportingService reportingService;
    private final MeterByCategory mbcAllResponse;
    private final MeterByCategory mbcAllTimeouts;


    public PowerFilterRouterImpl(DestinationLocationBuilder locationBuilder,
                                 Map<String, Destination> destinations,
                                 ReposeCluster domain,
                                 String defaultDestination,
                                 ServletContext servletContext,
                                 RequestHeaderService requestHeaderService,
                                 ResponseHeaderService responseHeaderService,
                                 MetricsService metricsService,
                                 ConcurrentHashMap<String, MeterByCategory> mapResponseCodes,
                                 ConcurrentHashMap<String, MeterByCategory> mapRequestTimeouts,
                                 ReportingService reportingService
    ) {

        this.locationBuilder = locationBuilder;
        this.destinations = destinations;
        this.domain = domain;
        this.defaultDestination = defaultDestination;
        this.servletContext = servletContext;
        this.requestHeaderService = requestHeaderService;
        this.responseHeaderService = responseHeaderService;
        this.metricsService = metricsService;
        this.mapResponseCodes = mapResponseCodes;
        this.mapRequestTimeouts = mapRequestTimeouts;
        this.reportingService = reportingService;

        mbcAllResponse = metricsService.newMeterByCategory(ResponseCode.class,
                "All Endpoints",
                "Response Codes",
                TimeUnit.SECONDS);

        mbcAllTimeouts = metricsService.newMeterByCategory(RequestTimeout.class,
                "TimeoutToOrigin",
                "Request Timeout",
                TimeUnit.SECONDS);
    }

    @Override
    public void route(MutableHttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException, ServletException, URISyntaxException {
        DestinationLocation location = null;

        if (!StringUtilities.isBlank(defaultDestination)) {
            servletRequest.addDestination(defaultDestination, servletRequest.getRequestURI(), -1);
        }

        RouteDestination routingDestination = servletRequest.getDestination();
        String rootPath = "";

        Destination configDestinationElement = null;

        if (routingDestination != null) {
            configDestinationElement = destinations.get(routingDestination.getDestinationId());
            if (configDestinationElement == null) {
                //TODO: do we really need domain? rename to cluster?
                LOG.warn("Invalid routing destination specified: {} for domain: {} ", routingDestination.getDestinationId(), domain.getId());
                servletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } else {
                location = locationBuilder.build(configDestinationElement, routingDestination.getUri(), servletRequest);
                rootPath = configDestinationElement.getRootPath();
            }
        }

        if (location != null) {
            // According to the Java 6 javadocs the routeDestination passed into getContext:
            // "The given path [routeDestination] must begin with /, is interpreted relative to the server's document root
            // and is matched against the context roots of other web applications hosted on this container."
            final ServletContext targetContext = servletContext.getContext(location.getUri().toString());

            if (targetContext != null) {
                // Capture this for Location header processing
                final HttpServletRequest originalRequest = (HttpServletRequest) servletRequest.getRequest();

                String uri = new DispatchPathBuilder(location.getUri().getPath(), targetContext.getContextPath()).build();
                final RequestDispatcher dispatcher = targetContext.getRequestDispatcher(uri);

                servletRequest.setRequestUrl(new StringBuffer(location.getUrl().toExternalForm()));
                servletRequest.setRequestUri(location.getUri().getPath()); //TODO: destination location builder is giving back an invalid URI
                requestHeaderService.setVia(servletRequest);
                requestHeaderService.setXForwardedFor(servletRequest);
                if (dispatcher != null) {
                    LOG.debug("Attempting to route to: {}", location.getUri());
                    LOG.debug("  Using dispatcher for: {}", uri);
                    LOG.debug("           Request URL: {}", servletRequest.getRequestURL());
                    LOG.debug("           Request URI: {}", servletRequest.getRequestURI());
                    LOG.debug("          Context path: {}", targetContext.getContextPath());

                    final long startTime = System.currentTimeMillis();
                    try {
                        reportingService.incrementRequestCount(routingDestination.getDestinationId());
                        dispatcher.forward(servletRequest, servletResponse);

                        // track response code for endpoint & across all endpoints
                        String endpoint = getEndpoint(configDestinationElement, location);
                        MeterByCategory mbc = verifyGet(endpoint);
                        MeterByCategory mbcTimeout = getTimeoutMeter(endpoint);

                        PowerFilter.markResponseCodeHelper(mbc, servletResponse.getStatus(), LOG, endpoint);
                        PowerFilter.markResponseCodeHelper(mbcAllResponse, servletResponse.getStatus(), LOG, MeterByCategorySum.ALL);
                        markRequestTimeoutHelper(mbcTimeout, servletResponse.getStatus(), endpoint);
                        markRequestTimeoutHelper(mbcAllTimeouts, servletResponse.getStatus(), "All Endpoints");

                        final long stopTime = System.currentTimeMillis();
                        reportingService.recordServiceResponse(routingDestination.getDestinationId(), servletResponse.getStatus(), stopTime - startTime);
                        responseHeaderService.fixLocationHeader(originalRequest, servletResponse, routingDestination, location.getUri().toString(), rootPath);
                    } catch (IOException e) {
                        if (e.getCause() instanceof ReadLimitReachedException) {
                            LOG.error("Error reading request content", e);
                            servletResponse.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Error reading request content");
                        } else {
                            LOG.error("Connection Refused to {}", location.getUri(), e);
                            servletResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                        }
                    }
                }
            }
        }
    }

    private String getEndpoint(Destination dest, DestinationLocation location) {
        StringBuilder sb = new StringBuilder();

        sb.append(location.getUri().getHost()).
                append(":").
                append(location.getUri().getPort());

        if (dest instanceof DestinationEndpoint) {
            sb.append(dest.getRootPath());
        } else if (dest instanceof DestinationCluster) {
            sb.append(dest.getRootPath());
        } else {
            throw new IllegalArgumentException("Unknown destination type: " + dest.getClass().getName());
        }

        return sb.toString();
    }

    private MeterByCategory verifyGet(String endpoint) {
        mapResponseCodes.putIfAbsent(endpoint, metricsService.newMeterByCategory(ResponseCode.class,
                endpoint,
                "Response Codes",
                TimeUnit.SECONDS));

        return mapResponseCodes.get(endpoint);
    }

    private MeterByCategory getTimeoutMeter(String endpoint) {
        mapRequestTimeouts.putIfAbsent(endpoint, metricsService.newMeterByCategory(RequestTimeout.class,
                "TimeoutToOrigin",
                "Request Timeout",
                TimeUnit.SECONDS));

        return mapRequestTimeouts.get(endpoint);
    }

    public void markRequestTimeoutHelper(MeterByCategory mbc, int responseCode, String endpoint) {
        if (mbc == null) {
            return;
        }

        if (responseCode == HTTP_CLIENT_TIMEOUT) {
            mbc.mark(endpoint);
        }
    }

}
